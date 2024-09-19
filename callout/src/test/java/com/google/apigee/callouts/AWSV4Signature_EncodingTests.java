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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AWSV4Signature_EncodingTests extends TestBase {

  @Override
  boolean isVerbose() {
    return true;
  }

  static class EncodingTestCase {
    public String input;
    public String expected;

    public EncodingTestCase(String input, String expected) {
      this.input = input;
      this.expected = expected;
    }
  }

  @DataProvider(name = "batch1")
  public Object[][] getDataForBatch1() {
    Object[][] cases =
        new Object[][] {
          new Object[] {new EncodingTestCase("case1", "case1")},
          new Object[] {
            new EncodingTestCase(
                "arn:aws:lambda:ca-central-1:992382745483:function:get-pets",
                "arn%3Aaws%3Alambda%3Aca-central-1%3A992382745483%3Afunction%3Aget-pets")
          }
        };

    return cases;
  }

  @Test(dataProvider = "batch1")
  public void uriEncodeTest(EncodingTestCase tc) throws Exception {
    System.out.printf("%s\n", tc.input);
    String actualEncodedResult = AWSV4Signature.uriEncode(tc.input, true);
    Assert.assertEquals(actualEncodedResult, tc.expected);
  }
}
