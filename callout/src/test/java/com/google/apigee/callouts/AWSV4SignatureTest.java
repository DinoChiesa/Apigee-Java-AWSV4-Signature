// AWSV4SignatureTest.java
//
// Test code for the AWS V4 signature  callout for Apigee. Uses TestNG.
// For full details see the Readme accompanying this source file.
//
// Copyright (c) 2016 Apigee Corp, 2017-2022 Google LLC
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

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AWSV4SignatureTest {
  private static final String testDataDir = "src/test/resources";
  private static final boolean verbose = true;

  static class Config {
    public static final String service = "service";
    public static final String region = "us-east-1";
    public static final String accessKeyId = "AKIDEXAMPLE";
    public static final String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
  }

  MessageContext msgCtxt;
  Message message;
  ExecutionContext exeCtxt;

  @BeforeMethod()
  public void testSetup1() {

    msgCtxt =
        new MockUp<MessageContext>() {
          private Map<String, Object> variables;

          public void $init() {
            getVariables();
          }

          private Map<String, Object> getVariables() {
            if (variables == null) {
              variables = new HashMap<String, Object>();
            }
            return variables;
          }

          @Mock()
          public Object getVariable(final String name) {
            return getVariables().get(name);
          }

          @Mock()
          public boolean setVariable(final String name, final Object value) {
            if (verbose)
              System.out.printf(
                  "setVariable(%s) <= %s\n", name, (value != null) ? value : "(null)");
            getVariables().put(name, value);
            return true;
          }

          @Mock()
          public boolean removeVariable(final String name) {
            if (verbose) System.out.printf("removeVariable(%s)\n", name);
            if (getVariables().containsKey(name)) {
              variables.remove(name);
            }
            return true;
          }
        }.getMockInstance();

    exeCtxt = new MockUp<ExecutionContext>() {}.getMockInstance();

    message =
        new MockUp<Message>() {
          private Map<String, Object> variables;
          private Map<String, Object> headers;
          private Map<String, Object> qparams;
          private String content;

          public void $init() {
            getVariables();
          }

          private Map<String, Object> getVariables() {
            if (variables == null) {
              variables = new HashMap<String, Object>();
            }
            return variables;
          }

          private Map<String, Object> getHeaders() {
            if (headers == null) {
              headers = new HashMap<String, Object>();
            }
            return headers;
          }

          private Map<String, Object> getQparams() {
            if (qparams == null) {
              qparams = new HashMap<String, Object>();
            }
            return qparams;
          }

          @Mock()
          public String getContent() {
            return this.content;
          }

          @Mock()
          public void setContent(String content) {
            this.content = content;
          }

          @Mock()
          public Object getVariable(final String name) {
            return getVariables().get(name);
          }

          @Mock()
          public boolean setVariable(final String name, final Object value) {
            getVariables().put(name, value);
            return true;
          }

          @Mock()
          public boolean removeVariable(final String name) {
            if (getVariables().containsKey(name)) {
              variables.remove(name);
            }
            return true;
          }

          @Mock()
          public String getHeader(final String name) {
            List<String> headerList = getHeaders(name);
            return (headerList != null) ? headerList.get(0) : null;
          }

          @Mock()
          public List<String> getHeaders(final String name) {
            String lowerName = name.toLowerCase();
            if (getHeaders().containsKey(lowerName)) {
              @SuppressWarnings("unchecked")
              List<String> list = (List<String>) getHeaders().get(lowerName);
              return list;
            }
            return null;
          }

          @Mock()
          public boolean setHeader(final String name, final Object value) {
            String lowerName = name.toLowerCase();
            if (verbose) {
              System.out.printf(
                  "setHeader(%s) <= %s\n", lowerName, (value != null) ? value : "(null)");
            }
            if (getHeaders().containsKey(lowerName)) {
              if (!lowerName.equals("host")) {
                @SuppressWarnings("unchecked")
                List<String> values = (List<String>) getHeaders().get(lowerName);
                values.add(value.toString());
              }
            } else {
              List<String> values = new ArrayList<String>();
              values.add(value.toString());
              getHeaders().put(lowerName, values);
            }
            return true;
          }

          @Mock()
          public boolean removeHeader(final String name) {
            String lowerName = name.toLowerCase();
            if (verbose) {
              System.out.printf("removeHeader(%s)\n", lowerName);
            }
            if (getHeaders().containsKey(lowerName)) {
              getHeaders().remove(lowerName);
            }
            return true;
          }

          @Mock()
          public Set<String> getHeaderNames() {
            return getHeaders().entrySet().stream()
                .map(e -> e.getKey())
                .collect(Collectors.toSet());
          }

          @Mock()
          public Set<String> getQueryParamNames() {
            return getQparams().entrySet().stream()
                .map(e -> e.getKey())
                .collect(Collectors.toSet());
          }

          @Mock()
          public String getQueryParam(final String name) {
            List<String> paramList = getQueryParams(name);
            return (paramList != null) ? paramList.get(0) : null;
          }

          @Mock()
          public boolean setQueryParam(final String name, final Object value) {
            if (verbose) {
              System.out.printf(
                  "setQueryParam(%s) <= %s\n", name, (value != null) ? value : "(null)");
            }
            if (getQparams().containsKey(name)) {
              @SuppressWarnings("unchecked")
              List<String> values = (List<String>) getQparams().get(name);
              values.add(value.toString());
            } else {
              List<String> values = new ArrayList<String>();
              values.add(value.toString());
              getQparams().put(name, values);
            }
            return true;
          }

          @Mock()
          public List<String> getQueryParams(final String name) {
            if (getQparams().containsKey(name)) {
              @SuppressWarnings("unchecked")
              List<String> list = (List<String>) getQparams().get(name);
              return list;
            }
            return null;
          }
        }.getMockInstance();

    System.out.printf("=============================================\n");
  }

  private void reportThings(Map<String, String> props) {
    String test = props.get("testname");
    System.out.println("test  : " + test);
    String cipher = msgCtxt.getVariable("crypto_cipher");
    System.out.println("cipher: " + cipher);
    String action = msgCtxt.getVariable("crypto_action");
    System.out.println("action: " + action);
    String output = msgCtxt.getVariable("crypto_output");
    System.out.println("output: " + output);
    String keyHex = msgCtxt.getVariable("crypto_key_b16");
    System.out.println("key   : " + keyHex);
    String ivHex = msgCtxt.getVariable("crypto_iv_b16");
    System.out.println("iv    : " + ivHex);
    String aadHex = msgCtxt.getVariable("crypto_aad_b16");
    System.out.println("aad   : " + aadHex);
    String saltHex = msgCtxt.getVariable("crypto_salt_b16");
    System.out.println("salt  : " + saltHex);
    // Assert.assertNotNull(ivHex);
    // Assert.assertNotNull(output);
  }

  @DataProvider(name = "batch1")
  public static Object[][] getDataForBatch1() throws IOException, IllegalStateException {

    // @DataProvider requires the output to be a Object[][]. The inner
    // Object[] is the set of params that get passed to the test method.
    // So, if you want to pass just one param to the constructor, then
    // each inner Object[] must have length 1.

    // Path currentRelativePath = Paths.get("");
    // String s = currentRelativePath.toAbsolutePath().toString();
    // System.out.println("Current relative path is: " + s);

    // read in all the subdirectories in the test-data directory

    File dataDir = new File(testDataDir);
    if (!dataDir.exists()) {
      throw new IllegalStateException("no test data directory.");
    }

    File[] dirs = dataDir.listFiles(File::isDirectory);
    if (dirs.length == 0) {
      throw new IllegalStateException("no tests found.");
    }
    Arrays.sort(dirs);
    Function<File, Object[]> toTestCase =
        (dir) -> {
          try {
            String name = dir.getName();
            return new Object[] {new TestCase(name, Paths.get(testDataDir, name))};
          } catch (java.lang.Exception exc1) {
            exc1.printStackTrace();
            throw new RuntimeException("uncaught exception", exc1);
          }
        };

    return Arrays.stream(dirs).map(toTestCase).toArray(Object[][]::new);
  }

  private String xform(String suffix) {
    return ((String) msgCtxt.getVariable("awsv4sig_" + suffix)).replaceAll("↵", "\n");
  }

  @Test
  public void testDataProviders() throws IOException {
    Assert.assertTrue(getDataForBatch1().length > 0);
  }

  @Test(dataProvider = "batch1")
  public void tests(TestCase tc) throws Exception {
    System.out.printf("%s\n", tc.getTestName());

    msgCtxt.setVariable("source", message);
    tc.parseInput(message);
    Properties props = new Properties();
    // props.setProperty("debug", "true");
    props.setProperty("debug", "true");
    props.setProperty("sign-content-sha256", "false");
    props.setProperty("source", "source");
    props.setProperty("key", Config.accessKeyId);
    props.setProperty("secret", Config.secretAccessKey);
    props.setProperty("region", Config.region);
    props.setProperty("service", Config.service);
    props.setProperty("endpoint", "https://" + message.getHeader("host"));

    AWSV4Signature callout = new AWSV4Signature(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;

    // check result and output
    Assert.assertEquals(actualResult, expectedResult, tc.getTestName() + " result not as expected");
    Assert.assertNull(msgCtxt.getVariable("awsv4sig_error"), tc.getTestName());
    Assert.assertEquals(xform("creq"), tc.canonicalRequest(), tc.getTestName());
    Assert.assertEquals(xform("sts"), tc.stringToSign(), tc.getTestName());
    Assert.assertEquals(message.getHeader("authorization"), tc.authorization(), tc.getTestName());
  }

  @Test()
  public void testS3_getRange() {
    // https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html
    final String testName = "testS3_getRange";
    final String creq =
        "GET\n"
            + "/test.txt\n"
            + "\n"
            + "host:examplebucket.s3.amazonaws.com\n"
            + "range:bytes=0-9\n"
            + "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n"
            + "x-amz-date:20130524T000000Z\n"
            + "\n"
            + "host;range;x-amz-content-sha256;x-amz-date\n"
            + "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    final String sts =
        "AWS4-HMAC-SHA256\n"
            + "20130524T000000Z\n"
            + "20130524/us-east-1/s3/aws4_request\n"
            + "7344ae5b7ee6c3e7e6b0fe0640412a37625d1fbfff95c48bbb2dc43964946972";

    final String authz =
        "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request,"
            + " SignedHeaders=host;range;x-amz-content-sha256;x-amz-date,"
            + " Signature=f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41";

    System.out.printf("%s\n", testName);
    msgCtxt.setVariable("source", message);
    message.setVariable("verb", "GET");
    message.setVariable("path", "/test.txt");
    message.setHeader("x-amz-date", "20130524T000000Z");
    message.setHeader("host", "examplebucket.s3.amazonaws.com");
    message.setHeader("Range", "bytes=0-9");

    Properties props = new Properties();
    // props.setProperty("debug", "true");
    props.setProperty("debug", "true");
    props.setProperty("sign-content-sha256", "true");
    props.setProperty("source", "source");
    props.setProperty("key", "AKIAIOSFODNN7EXAMPLE");
    props.setProperty("secret", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
    props.setProperty("region", "us-east-1");
    props.setProperty("service", "s3");
    props.setProperty("endpoint", "https://" + message.getHeader("host"));

    AWSV4Signature callout = new AWSV4Signature(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;

    // check result and output
    Assert.assertEquals(actualResult, expectedResult, testName + " result not as expected");
    Assert.assertNull(msgCtxt.getVariable("awsv4sig_error"), testName);
    Assert.assertEquals(xform("creq"), creq, testName);
    Assert.assertEquals(xform("sts"), sts, testName);
    Assert.assertEquals(message.getHeader("authorization"), authz, testName);
  }

  @Test()
  public void testS3_put() {
    // https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html
    final String testName = "testS3_put";
    final String creq =
        "PUT\n"
            + "/test%24file.text\n"
            + "\n"
            + "date:Fri, 24 May 2013 00:00:00 GMT\n"
            + "host:examplebucket.s3.amazonaws.com\n"
            + "x-amz-content-sha256:44ce7dd67c959e0d3524ffac1771dfbba87d2b6b4b4e99e42034a8b803f8b072\n"
            + "x-amz-date:20130524T000000Z\n"
            + "x-amz-storage-class:REDUCED_REDUNDANCY\n"
            + "\n"
            + "date;host;x-amz-content-sha256;x-amz-date;x-amz-storage-class\n"
            + "44ce7dd67c959e0d3524ffac1771dfbba87d2b6b4b4e99e42034a8b803f8b072";

    final String sts =
        "AWS4-HMAC-SHA256\n"
            + "20130524T000000Z\n"
            + "20130524/us-east-1/s3/aws4_request\n"
            + "9e0e90d9c76de8fa5b200d8c849cd5b8dc7a3be3951ddb7f6a76b4158342019d";

    final String authz =
        "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request,"
            + " SignedHeaders=date;host;x-amz-content-sha256;x-amz-date;x-amz-storage-class,"
            + " Signature=98ad721746da40c64f1a55b78f14c238d841ea1380cd77a1b5971af0ece108bd";

    System.out.printf("%s\n", testName);
    msgCtxt.setVariable("source", message);
    message.setVariable("verb", "PUT");
    message.setVariable("path", "/test$file.text");
    message.setHeader("date", "Fri, 24 May 2013 00:00:00 GMT");
    message.setHeader("x-amz-date", "20130524T000000Z");
    message.setHeader("host", "examplebucket.s3.amazonaws.com");
    message.setHeader("x-amz-storage-class", "REDUCED_REDUNDANCY");
    message.setContent("Welcome to Amazon S3.");

    Properties props = new Properties();
    // props.setProperty("debug", "true");
    props.setProperty("debug", "true");
    props.setProperty("sign-content-sha256", "true");
    props.setProperty("source", "source");
    props.setProperty("key", "AKIAIOSFODNN7EXAMPLE");
    props.setProperty("secret", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
    props.setProperty("region", "us-east-1");
    props.setProperty("service", "s3");
    props.setProperty("endpoint", "https://" + message.getHeader("host"));

    AWSV4Signature callout = new AWSV4Signature(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;

    // check result and output
    Assert.assertEquals(actualResult, expectedResult, testName + " result not as expected");
    Assert.assertNull(msgCtxt.getVariable("awsv4sig_error"), testName);
    Assert.assertEquals(xform("creq"), creq, testName);
    Assert.assertEquals(xform("sts"), sts, testName);
    Assert.assertEquals(message.getHeader("authorization"), authz, testName);
  }

  @Test()
  public void testS3_getQueryParams() {
    // https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html
    final String testName = "testS3_getQueryParams";
    final String creq =
        "GET\n"
            + "/\n"
            + "max-keys=2&prefix=J\n"
            + "host:examplebucket.s3.amazonaws.com\n"
            + "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n"
            + "x-amz-date:20130524T000000Z\n"
            + "\n"
            + "host;x-amz-content-sha256;x-amz-date\n"
            + "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    final String sts =
        "AWS4-HMAC-SHA256\n"
            + "20130524T000000Z\n"
            + "20130524/us-east-1/s3/aws4_request\n"
            + "df57d21db20da04d7fa30298dd4488ba3a2b47ca3a489c74750e0f1e7df1b9b7";

    final String authz =
        "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request,"
            + " SignedHeaders=host;x-amz-content-sha256;x-amz-date,"
            + " Signature=34b48302e7b5fa45bde8084f4b7868a86f0a534bc59db6670ed5711ef69dc6f7";

    System.out.printf("%s\n", testName);
    msgCtxt.setVariable("source", message);
    message.setVariable("verb", "GET");
    message.setVariable("path", "/");
    message.setHeader("x-amz-date", "20130524T000000Z");
    message.setHeader("host", "examplebucket.s3.amazonaws.com");
    message.setQueryParam("max-keys", "2");
    message.setQueryParam("prefix", "J");

    Properties props = new Properties();
    // props.setProperty("debug", "true");
    props.setProperty("debug", "true");
    props.setProperty("sign-content-sha256", "true");
    props.setProperty("source", "source");
    props.setProperty("key", "AKIAIOSFODNN7EXAMPLE");
    props.setProperty("secret", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
    props.setProperty("region", "us-east-1");
    props.setProperty("service", "s3");
    props.setProperty("endpoint", "https://" + message.getHeader("host"));

    AWSV4Signature callout = new AWSV4Signature(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;

    // check result and output
    Assert.assertEquals(actualResult, expectedResult, testName + " result not as expected");
    Assert.assertNull(msgCtxt.getVariable("awsv4sig_error"), testName);
    Assert.assertEquals(xform("creq"), creq, testName);
    Assert.assertEquals(xform("sts"), sts, testName);
    Assert.assertEquals(message.getHeader("authorization"), authz, testName);
  }

  @Test()
  public void testS3_no_source() {

    final String testName = "testS3_no_source";
    System.out.printf("%s\n", testName);

    Properties props = new Properties();
    // props.setProperty("source", "source"); // no source!
    props.setProperty("key", "AKIAIOSFODNN7EXAMPLE");
    props.setProperty("secret", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
    props.setProperty("region", "us-east-1");
    props.setProperty("service", "s3");
    props.setProperty("endpoint", "https://" + message.getHeader("host"));

    AWSV4Signature callout = new AWSV4Signature(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.ABORT;

    // check result and output
    Assert.assertEquals(actualResult, expectedResult, testName + " result not as expected");
    Assert.assertEquals(
        msgCtxt.getVariable("awsv4sig_error"), "neither source nor verb is specified.", testName);
    Assert.assertNull(msgCtxt.getVariable("awsv4sig_creq"), testName);
    Assert.assertNull(msgCtxt.getVariable("awsv4sig_sts"), testName);
    Assert.assertNull(message.getHeader("authorization"), testName);
  }

  @Test()
  public void testS3_presigned_url() {
    // https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-query-string-auth.html#query-string-auth-v4-signing-example
    final String testName = "testS3_presigned_url";
    final String creq =
        "GET\n"
            + "/test.txt\n"
            + "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F20130524%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20130524T000000Z&X-Amz-Expires=86400&X-Amz-SignedHeaders=host\n"
            + "host:examplebucket.s3.amazonaws.com\n"
            + "\n"
            + "host\n"
            + "UNSIGNED-PAYLOAD";

    final String sts =
        "AWS4-HMAC-SHA256\n"
            + "20130524T000000Z\n"
            + "20130524/us-east-1/s3/aws4_request\n"
            + "3bfa292879f6447bbcda7001decf97f4a54dc650c8942174ae0a9121cf58ad04";

    final String signature = "aeeed9bbccd4d02ee5c0109b86d86835f995330da4c265957d157751f604d404";

    final String constructedUrl =
        "https://examplebucket.s3.amazonaws.com/test.txt?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F20130524%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20130524T000000Z&X-Amz-Expires=86400&X-Amz-SignedHeaders=host&X-Amz-Signature=aeeed9bbccd4d02ee5c0109b86d86835f995330da4c265957d157751f604d404";

    System.out.printf("%s\n", testName);

    Properties props = new Properties();
    // props.setProperty("debug", "true");
    props.setProperty("debug", "true");
    // props.setProperty("source", "source"); // no source!
    props.setProperty("request-verb", "GET");
    props.setProperty("request-path", "/test.txt");
    props.setProperty("request-date", "20130524T000000Z");
    props.setProperty("request-expiry", "86400");
    props.setProperty("output", "my_output");

    props.setProperty("key", "AKIAIOSFODNN7EXAMPLE");
    props.setProperty("secret", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
    props.setProperty("region", "us-east-1");
    props.setProperty("service", "s3");
    props.setProperty("endpoint", "https://examplebucket.s3.amazonaws.com");

    AWSV4Signature callout = new AWSV4Signature(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;

    // check result and output
    Assert.assertEquals(actualResult, expectedResult, testName + " result not as expected");
    Assert.assertNull(msgCtxt.getVariable("awsv4sig_error"), testName);
    Assert.assertEquals(xform("creq"), creq, testName);
    Assert.assertEquals(xform("sts"), sts, testName);
    Assert.assertEquals(msgCtxt.getVariable("my_output"), constructedUrl, testName);
  }

  @Test()
  public void post_insure_trailing_slash() {
    final String testName = "post_insure_trailing_slash";
    final String apiKey = "N0j13d61b5f13a54916a4dbE1D949BcbB6qmzy69";
    final String timestamp = "20210609T065036Z";
    final String hostname = "stage.exampleapi.com";
    final String expectedCreqTmpl =
        "POST\n"
            + "/v1/LookupUser/\n"
            + "\n"
            + "content-type:application/json\n"
            + "host:@@HOST@@\n"
            + "x-amz-content-sha256:902886a3ffe631cba0d08501244ba1edf18b378923d4ef9c4b59d671307b3d5b\n"
            + "x-amz-date:@@TIMESTAMP@@\n"
            + "x-api-key:@@APIKEY@@\n"
            + "\n"
            + "content-type;host;x-amz-content-sha256;x-amz-date;x-api-key\n"
            + "902886a3ffe631cba0d08501244ba1edf18b378923d4ef9c4b59d671307b3d5b";

    final String expectedCreq =
        expectedCreqTmpl
            .replaceAll("@@HOST@@", hostname)
            .replaceAll("@@TIMESTAMP@@", timestamp)
            .replaceAll("@@APIKEY@@", apiKey);

    final String expectedSts =
        "AWS4-HMAC-SHA256\n"
            + "20210609T065036Z\n"
            + "20210609/us-west-2/execute-api/aws4_request\n"
            + "e264117df531c38f6fe540bb47c89a95595f426a2730db28c7b0fcb4239c733e";

    // check for no double slash when insure-trailing-slash is true
    final String[] cases = {"/v1/LookupUser", "/v1/LookupUser/"};

    for (String urlpath : cases) {
      System.out.printf("%s (case %s)\n", testName, urlpath);
      msgCtxt.setVariable("source", message);
      message.setVariable("verb", "POST");
      message.setVariable("path", urlpath);
      // reset headers
      for (String hdrName : message.getHeaderNames()) {
        message.removeHeader(hdrName);
      }

      message.setHeader("content-type", "application/json");
      message.setHeader("x-api-key", apiKey);
      message.setHeader("x-amz-date", timestamp);
      message.setHeader("host", hostname);
      message.setContent("{ \"foo\" : \"bar\" }");

      Properties props = new Properties();
      // props.setProperty("debug", "true");
      props.setProperty("debug", "true");
      props.setProperty("sign-content-sha256", "true");
      props.setProperty("insure-trailing-slash", "true");
      props.setProperty("source", "source");
      props.setProperty("key", "AKIAIOSFODNN7EXAMPLE");
      props.setProperty("secret", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
      props.setProperty("region", "us-west-2");
      props.setProperty("service", "execute-api");
      props.setProperty("endpoint", "https://" + message.getHeader("host"));

      AWSV4Signature callout = new AWSV4Signature(props);

      // execute and retrieve output
      ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
      ExecutionResult expectedResult = ExecutionResult.SUCCESS;

      // check result and output
      Assert.assertEquals(actualResult, expectedResult, testName + " result not as expected");
      Assert.assertNull(msgCtxt.getVariable("awsv4sig_error"), testName);
      String actualCreq = xform("creq");
      System.out.printf("actualCreq: %s\n", actualCreq);
      Assert.assertEquals(actualCreq, expectedCreq, testName);
      String actualSts = xform("sts");
      System.out.printf("actualSts: %s\n", actualSts);
      Assert.assertEquals(actualSts, expectedSts, testName);
    }
  }
}
