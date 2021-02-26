// AWSV4Signature.java
//
// This is the main callout class for the AES Crypto custom policy for Apigee Edge.
// For full details see the Readme accompanying this source file.
//
// Copyright (c) 2021 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// @author: Dino Chiesa
//

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AWSV4Signature extends SignatureCalloutBase implements Execution {
  protected static final DateTimeFormatter xAmzDateFormatter =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
  protected static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

  public AWSV4Signature(Map properties) {
    super(properties);
  }

  protected static String hex(byte[] a) {
    // return org.bouncycastle.util.encoders.Hex.toHexString(a);
    return com.google.apigee.encoding.Base16.encode(a);
  }

  protected static byte[] sha256(String s) throws java.security.NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] output = digest.digest(s.getBytes(StandardCharsets.UTF_8));
    return output;
  }

  static byte[] hmacsha256(String data, byte[] key) throws Exception {
    String algorithm = "HmacSHA256";
    Mac mac = Mac.getInstance(algorithm);
    mac.init(new SecretKeySpec(key, algorithm));
    return mac.doFinal(data.getBytes("UTF-8"));
  }

  static byte[] getSigningKey(String key, String dateStamp, String regionName, String serviceName)
      throws Exception {
    byte[] kSecret = ("AWS4" + key).getBytes("UTF-8");
    byte[] kDate = hmacsha256(dateStamp, kSecret);
    byte[] kRegion = hmacsha256(regionName, kDate);
    byte[] kService = hmacsha256(serviceName, kRegion);
    byte[] kSigning = hmacsha256("aws4_request", kService);
    return kSigning;
  }

  private Message getSource(MessageContext msgCtxt) {
    String sourceVar = _getRequiredString(msgCtxt, "source");
    Message sourceMessage = (Message) msgCtxt.getVariable(sourceVar);
    if (sourceMessage == null)
      throw new IllegalStateException("source does not resolve to a message.");
    return sourceMessage;
  }

  protected boolean wantSignedContentSha256(MessageContext msgCtxt) throws Exception {
    return _getBooleanProperty(msgCtxt, "sign-content-sha256", false);
  }

  private String getService(MessageContext msgCtxt) {
    return _getRequiredString(msgCtxt, "service");
  }

  private String getRegion(MessageContext msgCtxt) {
    return _getRequiredString(msgCtxt, "region");
  }

  private String getKey(MessageContext msgCtxt) {
    return _getRequiredString(msgCtxt, "key");
  }

  private String getSecret(MessageContext msgCtxt) {
    return _getRequiredString(msgCtxt, "secret");
  }

  private String getEndpoint(MessageContext msgCtxt) {
    return _getRequiredString(msgCtxt, "endpoint");
  }

  private static void clearVariables(MessageContext msgCtxt) {
    msgCtxt.removeVariable(varName("error"));
    msgCtxt.removeVariable(varName("exception"));
    msgCtxt.removeVariable(varName("stacktrace"));
  }

  static class Canonicalized {
    public String signedHeaders;
    public String request;

    public Canonicalized(String headers, String request) {
      this.signedHeaders = headers;
      this.request = request;
    }
  }

  public static String encodeURIComponent(String s) {
    String result;
    try {
      result =
          URLEncoder.encode(s, "UTF-8")
              .replaceAll("\\+", "%20")
              .replaceAll("\\%21", "!")
              .replaceAll("\\%27", "'")
              .replaceAll("\\%28", "(")
              .replaceAll("\\%29", ")")
              .replaceAll("\\%7E", "~");
    } catch (UnsupportedEncodingException e) {
      result = s;
    }
    return result;
  }

  public static String encodeSpaces(String s) {
    return s.replaceAll(" ", "%20");
  }

  public static String normalizeSpace(String s) {
    return s.trim().replaceAll("( )+", " ");
  }

  public static String normalizePath(String s) {
    String normalizedPath = Paths.get("/", s).normalize().toString();
    if (normalizedPath.length() > 1 && s.endsWith("/")) {
      normalizedPath += "/";
    }
    return normalizedPath;
  }

  private static Canonicalized getCanonicalRequest(Message message, String contentSha256) {
    // (1) https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
    List<String> canonicalRequestLines = new ArrayList<>();
    canonicalRequestLines.add(message.getVariable("verb").toString().toUpperCase());
    canonicalRequestLines.add(encodeSpaces(normalizePath(message.getVariable("path").toString())));

    List<String> encodedQparams = new ArrayList<String>();
    List<String> qparamList = new ArrayList<String>(message.getQueryParamNames());
    Collections.sort(qparamList);
    for (String paramName : qparamList) {
      List<String> paramValues = message.getQueryParams(paramName);
      Collections.sort(paramValues);
      for (String paramValue : paramValues) {
        encodedQparams.add(encodeURIComponent(paramName) + "=" + encodeURIComponent(paramValue));
      }
    }
    canonicalRequestLines.add(encodedQparams.stream().collect(Collectors.joining("&")));

    List<String> hashedHeaders = new ArrayList<>();
    List<String> headerList = new ArrayList<String>(message.getHeaderNames());
    Collections.sort(headerList);
    for (String headerName : headerList) {
      List<String> headerValues = message.getHeaders(headerName);
      hashedHeaders.add(headerName.toLowerCase());
      String joinedValue =
          headerValues.stream().map(s -> normalizeSpace(s)).collect(Collectors.joining(","));
      canonicalRequestLines.add(headerName.toLowerCase() + ":" + joinedValue);
    }
    canonicalRequestLines.add(null); // new line required after headers

    String signedHeaders = hashedHeaders.stream().collect(Collectors.joining(";"));
    canonicalRequestLines.add(signedHeaders);
    if (contentSha256 != null) {
      canonicalRequestLines.add(contentSha256);
    }
    return new Canonicalized(
        signedHeaders,
        canonicalRequestLines.stream()
            .map(line -> line == null ? "" : line)
            .collect(Collectors.joining("\n")));
  }

  public ExecutionResult execute(MessageContext msgCtxt, ExecutionContext exeCtxt) {
    boolean debug = false;
    try {
      clearVariables(msgCtxt);
      debug = getDebug(msgCtxt);
      String dateTimeStamp = null;
      Message sourceMessage = getSource(msgCtxt);
      String content = sourceMessage.getContent();

      String contentSha256 =
          (sourceMessage.getContent() == null)
              ? hex(sha256(""))
              : hex(sha256(sourceMessage.getContent()));
      String endpoint = getEndpoint(msgCtxt);

      if (sourceMessage.getHeader("x-amz-date") != null) {
        dateTimeStamp = sourceMessage.getHeader("x-amz-date");
      } else {
        ZonedDateTime now = ZonedDateTime.now();
        dateTimeStamp = xAmzDateFormatter.format(now);
        sourceMessage.setHeader("x-amz-date", dateTimeStamp);
      }
      if (wantSignedContentSha256(msgCtxt)) {
        sourceMessage.setHeader("x-amz-content-sha256", contentSha256);
      }
      sourceMessage.setHeader("Host", endpoint.substring(8));

      final Canonicalized canonicalized = getCanonicalRequest(sourceMessage, contentSha256);
      msgCtxt.setVariable(varName("creq"), canonicalized.request);

      String dateStamp = dateTimeStamp.substring(0, 8);
      String region = getRegion(msgCtxt);
      String service = getService(msgCtxt);
      String scope = dateStamp + "/" + region + "/" + service + "/aws4_request";

      List<String> strignToSignLines = new ArrayList<>();
      strignToSignLines.add("AWS4-HMAC-SHA256");
      strignToSignLines.add(dateTimeStamp);
      strignToSignLines.add(scope);
      strignToSignLines.add(hex(sha256(canonicalized.request)));
      String stringToSign = strignToSignLines.stream().collect(Collectors.joining("\n"));
      msgCtxt.setVariable(varName("sts"), stringToSign);

      byte[] signingKey = getSigningKey(getSecret(msgCtxt), dateStamp, region, service);

      final byte[] signature = hmacsha256(stringToSign, signingKey);
      final String credentialsAuthorizationHeader = "Credential=" + getKey(msgCtxt) + "/" + scope;
      final String signedHeadersAuthorizationHeader =
          "SignedHeaders=" + canonicalized.signedHeaders;
      final String signatureAuthorizationHeader = "Signature=" + hex(signature);

      sourceMessage.setHeader(
          "Authorization",
          "AWS4-HMAC-SHA256 "
              + credentialsAuthorizationHeader
              + ", "
              + signedHeadersAuthorizationHeader
              + ", "
              + signatureAuthorizationHeader);
    } catch (Exception e) {
      if (debug) {
        // e.printStackTrace();
        msgCtxt.setVariable(varName("stacktrace"), exceptionStackTrace(e));
      }
      setExceptionVariables(e, msgCtxt);
      return ExecutionResult.ABORT;
    }
    return ExecutionResult.SUCCESS;
  }
}
