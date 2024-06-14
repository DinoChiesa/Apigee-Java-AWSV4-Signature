// Copyright © 2016 Apigee Corp, 2017-2024 Google LLC
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

import com.apigee.flow.execution.ExecutionResult;
import java.util.Properties;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AWSV4Signature_Tests2 extends TestBase {

  @Override
  boolean isVerbose() {
    return true;
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

  @Test()
  public void oddProperties() {
    // https://www.googlecloudcommunity.com/gc/Apigee/S3-Integration-with-ApigeeX/m-p/635654#M77215
    final String testName = "oddProperties";
    System.out.printf("%s\n", testName);
    msgCtxt.setVariable("outgoingAwsMessage", message);

    Properties props = new Properties();
    // props.setProperty("debug", "true");
    props.setProperty("service", "s3");
    props.setProperty("endpoint", "https://s3.eu-west-1.amazonaws.com");
    props.setProperty("region", "eu-west-1");
    props.setProperty("key", "\n        <Value ref=\"private.key\"/>\n        ");
    props.setProperty("secret", "\n        <Value ref=\"private.secret\"/>\n        ");
    props.setProperty("source", "outgoingAwsMessage");
    props.setProperty("sign-content-sha256", "true");
    props.setProperty("debug", "true");

    AWSV4Signature callout = new AWSV4Signature(props);
    Assert.assertNotNull(callout, testName);
  }

  @Test()
  public void noValueQueryParam() {
    final String testName = "noValueQueryParam";
    final String creq =
        "GET\n"
            + "/\n"
            + "p1=1&p2\n"
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
            + "85530f2bc80d774ba886609ae46dd8fc796e3bacf7951d4603193232a65cc38e";

    final String authz =
        "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request,"
            + " SignedHeaders=host;x-amz-content-sha256;x-amz-date,"
            + " Signature=8dddef6f510650d1e7bae0da461360554c6d6113a85542dc3f2d33821df28473";

    System.out.printf("%s\n", testName);
    msgCtxt.setVariable("source", message);
    message.setVariable("verb", "GET");
    message.setVariable("path", "/");
    message.setHeader("x-amz-date", "20130524T000000Z");
    message.setHeader("host", "examplebucket.s3.amazonaws.com");
    message.setQueryParam("p1", "1");
    message.setQueryParam("p2", "");

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
}
