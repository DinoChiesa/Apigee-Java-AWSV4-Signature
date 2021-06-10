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
import com.google.apigee.encoding.Base16;
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
import java.util.TreeMap;
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
    return Base16.encode(a);
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

  private Message getSource(MessageContext msgCtxt) {
    String sourceVar = _getOptionalString(msgCtxt, "source");
    if (sourceVar == null) {
      return null;
    }

    Message sourceMessage = (Message) msgCtxt.getVariable(sourceVar);
    return sourceMessage;
  }

  protected boolean wantInsureTrailingSlashOnPath(MessageContext msgCtxt) throws Exception {
    return _getBooleanProperty(msgCtxt, "insure-trailing-slash", false);
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
    msgCtxt.removeVariable(varName("creq"));
    msgCtxt.removeVariable(varName("sts"));
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

  public static String uriEncode(CharSequence input, boolean encodeSlash) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char ch = input.charAt(i);
      if ((ch >= 'A' && ch <= 'Z')
          || (ch >= 'a' && ch <= 'z')
          || (ch >= '0' && ch <= '9')
          || ch == '_'
          || ch == '-'
          || ch == '~'
          || ch == '.') {
        result.append(ch);
      } else if (ch == '/') {
        result.append(encodeSlash ? "%2F" : ch);
      } else {
        result.append("%").append(Base16.byteToHex((byte) ch));
      }
    }
    return result.toString();
  }

  public static String normalizeSpace(String s) {
    return s.trim().replaceAll("( )+", " ");
  }

  public class SignConfiguration {
    Message sourceMessage;
    String endpoint;
    String host;
    String verb;
    String path;
    String dateTimeStamp;
    String region;
    String service;
    String dateStamp;
    String contentSha256;
    String secret;
    String key;
    boolean wantSignedContentSha256;
    boolean insureTrailingSlashOnPath;
    boolean debug;
    String scope;
    String stringToSign;
    String output;
    String expiry;
    MessageContext msgCtxt;
    List<String> encodedQparams = new ArrayList<String>();
    Map<String, String> headers = new TreeMap<String, String>();

    private void applyDate(String dateOverride) {
      if (dateOverride != null) {
        this.dateTimeStamp = dateOverride;
      } else {
        ZonedDateTime now = ZonedDateTime.now();
        this.dateTimeStamp = xAmzDateFormatter.format(now);
      }
      this.dateStamp = this.dateTimeStamp.substring(0, 8);
    }

    private void setScope() {
      scope = this.dateStamp + "/" + this.region + "/" + this.service + "/aws4_request";
    }

    public SignConfiguration(MessageContext msgCtxt, boolean debug) throws Exception {
      wantSignedContentSha256 = false;
      this.debug = debug;
      this.msgCtxt = msgCtxt;

      endpoint = getEndpoint(msgCtxt);
      host = endpoint.substring(8);
      // TODO: validate the endpoint. Does it look like an https url?
      region = getRegion(msgCtxt);
      service = getService(msgCtxt);
      secret = getSecret(msgCtxt);
      insureTrailingSlashOnPath = wantInsureTrailingSlashOnPath(msgCtxt);
      key = getKey(msgCtxt);

      sourceMessage = getSource(msgCtxt);

      if (sourceMessage != null) {
        // get configuration from a previously created message
        verb = sourceMessage.getVariable("verb").toString().toUpperCase();
        path = sourceMessage.getVariable("path").toString();
        applyDate(sourceMessage.getHeader("x-amz-date"));

        String content = sourceMessage.getContent();
        contentSha256 = (content == null) ? hex(sha256("")) : hex(sha256(content));

        this.wantSignedContentSha256 = wantSignedContentSha256(msgCtxt);
        if (wantSignedContentSha256) {
          headers.put("x-amz-content-sha256", contentSha256);
        }

        // pre-process headers
        List<String> headerList = new ArrayList<String>(sourceMessage.getHeaderNames());
        Collections.sort(headerList);
        for (String headerName : headerList) {
          List<String> headerValues = sourceMessage.getHeaders(headerName);
          String joinedValue =
              headerValues.stream().map(s -> normalizeSpace(s)).collect(Collectors.joining(","));
          headers.put(headerName.toLowerCase(), joinedValue);
        }
        if (!headers.containsKey("host")) {
          headers.put("host", host);
        }
        if (!headers.containsKey("x-amz-date")) {
          headers.put("x-amz-date", dateTimeStamp);
        }

        // pre-process qparams
        List<String> qparamList = new ArrayList<String>(sourceMessage.getQueryParamNames());
        Collections.sort(qparamList);
        for (String paramName : qparamList) {
          List<String> unmodifiableList = sourceMessage.getQueryParams(paramName);
          List<String> paramValues = new ArrayList<String>(unmodifiableList);
          Collections.sort(paramValues);
          for (String paramValue : paramValues) {
            encodedQparams.add(
                encodeURIComponent(paramName) + "=" + encodeURIComponent(paramValue));
          }
        }
        setScope();

      } else {
        // get config from individual properties
        verb = _getOptionalString(msgCtxt, "request-verb");
        path = _getOptionalString(msgCtxt, "request-path");
        applyDate(_getOptionalString(msgCtxt, "request-date"));
        contentSha256 = hex(sha256(""));

        if (path == null) {
          throw new IllegalStateException("neither source nor verb is specified.");
        }
        if (path == null) {
          throw new IllegalStateException("neither source nor path is specified.");
        }

        expiry = _getRequiredString(msgCtxt, "request-expiry");
        // TODO: perform time validation and resolution here

        output = _getRequiredString(msgCtxt, "output");

        setScope();

        // pre-load qparams here
        encodedQparams.add("X-Amz-Algorithm=AWS4-HMAC-SHA256");
        encodedQparams.add("X-Amz-Credential=" + uriEncode(key + "/" + scope, true));
        encodedQparams.add("X-Amz-Date=" + dateTimeStamp);
        encodedQparams.add("X-Amz-Expires=" + expiry);
        encodedQparams.add("X-Amz-SignedHeaders=host");
      }
    }

    public String computeStringToSign(Canonicalized canonicalized) throws Exception {
      List<String> stringToSignLines = new ArrayList<>();
      stringToSignLines.add("AWS4-HMAC-SHA256");
      stringToSignLines.add(dateTimeStamp);
      stringToSignLines.add(scope);
      stringToSignLines.add(hex(sha256(canonicalized.request)));
      stringToSign = stringToSignLines.stream().collect(Collectors.joining("\n"));
      return stringToSign;
    }

    public byte[] getSigningKey() throws Exception {
      byte[] kSecret = ("AWS4" + secret).getBytes("UTF-8");
      byte[] kDate = hmacsha256(dateStamp, kSecret);
      byte[] kRegion = hmacsha256(region, kDate);
      byte[] kService = hmacsha256(service, kRegion);
      byte[] kSigning = hmacsha256("aws4_request", kService);
      return kSigning;
    }

    private void setHeader(String headerName, String value, boolean sensitive) {
      sourceMessage.setHeader(headerName, value);
      if (debug || !sensitive) {
        msgCtxt.setVariable(varName("header." + headerName), value);
      }
    }

    public void emitOutput(Canonicalized canonicalized) throws Exception {
      final byte[] signature = hmacsha256(stringToSign, getSigningKey());

      if (sourceMessage != null) {
        if (wantSignedContentSha256) {
          setHeader("x-amz-content-sha256", contentSha256, false);
        }

        setHeader("x-amz-date", dateTimeStamp, false);
        setHeader("Host", host, false);
        String credentials = "Credential=" + key + "/" + scope;
        String signedHeaders = "SignedHeaders=" + canonicalized.signedHeaders;
        String signatureString = "Signature=" + hex(signature);

        String authzHeader =
            "AWS4-HMAC-SHA256 " + credentials + ", " + signedHeaders + ", " + signatureString;
        setHeader("Authorization", authzHeader, true);
      } else {
        encodedQparams.add("X-Amz-Signature=" + hex(signature));

        String constructedUrl =
            String.format(
                "%s%s?%s",
                endpoint, path, encodedQparams.stream().collect(Collectors.joining("&")));
        msgCtxt.setVariable(output, constructedUrl);
      }
    }

    private String normalizePath(String s) {
      String normalizedPath = Paths.get("/", s).normalize().toString();
      if (normalizedPath.length() > 1 && s.endsWith("/")) {
        normalizedPath += "/";
      }
      else if (insureTrailingSlashOnPath) {
        normalizedPath += "/";
      }
      return normalizedPath;
    }

    public Canonicalized getCanonicalRequest() {
      // (1) https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
      List<String> canonicalRequestLines = new ArrayList<>();
      canonicalRequestLines.add(this.verb);
      canonicalRequestLines.add(uriEncode(normalizePath(this.path), false));
      canonicalRequestLines.add(this.encodedQparams.stream().collect(Collectors.joining("&")));

      String signedHeaders = null;
      if (this.sourceMessage != null) {
        for (Map.Entry<String, String> entry : this.headers.entrySet()) {
          canonicalRequestLines.add(entry.getKey() + ":" + entry.getValue());
        }
        signedHeaders = this.headers.keySet().stream().collect(Collectors.joining(";"));
      } else {
        // this is for constructing a presigned URL for a GET request; there are no headers
        canonicalRequestLines.add("host:" + this.host);
        signedHeaders = "host";
      }
      canonicalRequestLines.add(null); // new line required after headers
      canonicalRequestLines.add(signedHeaders);

      if (this.sourceMessage != null) {
        canonicalRequestLines.add(this.contentSha256);
      } else {
        canonicalRequestLines.add("UNSIGNED-PAYLOAD");
      }
      return new Canonicalized(
          signedHeaders,
          canonicalRequestLines.stream()
              .map(line -> line == null ? "" : line)
              .collect(Collectors.joining("\n")));
    }
  }

  public ExecutionResult execute(MessageContext msgCtxt, ExecutionContext exeCtxt) {
    boolean debug = false;
    try {
      clearVariables(msgCtxt);
      debug = getDebug(msgCtxt);
      SignConfiguration signConfig = new SignConfiguration(msgCtxt, debug);
      final Canonicalized canonicalized = signConfig.getCanonicalRequest();
      msgCtxt.setVariable(varName("creq"), canonicalized.request.replaceAll("\n","↵"));

      final String stringToSign = signConfig.computeStringToSign(canonicalized);
      msgCtxt.setVariable(varName("sts"), stringToSign.replaceAll("\n","↵"));

      signConfig.emitOutput(canonicalized);

    } catch (Exception e) {
      if (debug) {
        e.printStackTrace();
        msgCtxt.setVariable(varName("stacktrace"), exceptionStackTrace(e));
      }
      setExceptionVariables(e, msgCtxt);
      return ExecutionResult.ABORT;
    }
    return ExecutionResult.SUCCESS;
  }
}
