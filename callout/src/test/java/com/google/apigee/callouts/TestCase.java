// Copyright 2017-2021 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.google.apigee.callouts;

import com.apigee.flow.message.Message;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestCase implements Comparable {

  private String findFileByExt(String extension) {
    File[] allFiles = _path.toFile().listFiles(File::isFile);
    String fname =
        Arrays.asList(allFiles).stream()
            .filter(item -> item.getName().endsWith(extension))
            .findFirst()
            .get()
            .getName();
    return fname;
  }

  public TestCase(String name, Path path) throws java.io.IOException {
    this._testName = name;
    this._path = path;
    String fname = findFileByExt(".req");
    this._input = readAll(Paths.get(_path.toString(), fname));
  }

  public static String readAll(Path path) throws java.io.IOException {
    return new String(Files.readAllBytes(path));
  }

  private String _testName;
  private String _input; // content of request
  //  private String _path;
  private Path _path;

  // getters
  public String getTestName() {
    return _testName;
  }

  public String getInput() {
    return _input;
  }

  // setters
  public void setTestName(String n) {
    _testName = n;
  }

  public void setInput(String f) {
    _input = f;
  }

  public void parseInput(Message message) {
    String[] lines = _input.split("\\n");
    String[] parts = lines[0].split(" ", 2);
    message.setVariable("verb", parts[0]);

    String pathQueryAndProtocol = parts[1].trim();
    parts = pathQueryAndProtocol.split(" ");
    String pathAndQuery =
        Arrays.asList(parts).stream().limit(parts.length - 1).collect(Collectors.joining(" "));

    parts = pathAndQuery.split("\\?", 2);
    message.setVariable("path", parts[0]);

    if (parts.length == 2) {
      String query = parts[1];
      String[] nameValuePairs = query.split("&");
      for (String nameValuePair : nameValuePairs) {
        parts = nameValuePair.split("=", 2);
        if (parts.length == 2) message.setQueryParam(parts[0], parts[1]);
        else message.setQueryParam(parts[0], null);
      }
    }

    List<String> payloadLines = null;
    for (int i = 1; i < lines.length; i++) {
      String line = lines[i];
      if (payloadLines == null) {
        if (line.trim().length() > 1) {
          String[] hparts = lines[i].split(":", 2);
          if (hparts.length == 2) {
            message.setHeader(hparts[0].toLowerCase(), hparts[1]);
          } else {
            message.setHeader(hparts[0].toLowerCase(), "");
          }
        } else {
          payloadLines = new ArrayList<String>();
        }
      } else {
        payloadLines.add(line);
      }
    }

    if (payloadLines != null && payloadLines.size() > 0) {
      message.setContent(payloadLines.stream().collect(Collectors.joining("\n")));
    }
  }

  public String stringToSign() throws java.io.IOException {
    return readAll(Paths.get(_path.toString(), findFileByExt(".sts")));
  }

  public String canonicalRequest() throws java.io.IOException {
    return readAll(Paths.get(_path.toString(), findFileByExt(".creq")));
  }

  public String authorization() throws java.io.IOException {
    String sreq = readAll(Paths.get(_path.toString(), findFileByExt(".sreq")));
    String authzLine =
        Arrays.asList(sreq.split("\\n")).stream()
            .filter(s -> s.toLowerCase().startsWith("authorization:"))
            .findFirst()
            .get();
    if (authzLine == null) {
      throw new IllegalStateException("missing authorization header");
    }
    String[] parts = authzLine.split(":", 2);
    return parts[1].trim();
  }

  @Override
  public int compareTo(Object tc) {
    return getTestName().compareTo(((TestCase) tc).getTestName());
  }
}
