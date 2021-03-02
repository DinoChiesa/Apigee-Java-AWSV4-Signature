// Copyright 2018-2021 Google LLC
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

import com.apigee.flow.message.MessageContext;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SignatureCalloutBase {
  private static final String _varprefix = "awsv4sig_";
  private static final String variableReferencePatternString =
      "(.*?)\\{([^\\{\\} :][^\\{\\} ]*?)\\}(.*?)";
  private static final Pattern variableReferencePattern =
      Pattern.compile(variableReferencePatternString);
  private static final String commonError = "^(.+?)[:;] (.+)$";
  private static final Pattern commonErrorPattern = Pattern.compile(commonError);
  protected final Map<String, String> properties;

  public SignatureCalloutBase(Map properties) {
    this.properties = genericizeMap(properties);
  }

  protected static String varName(String s) {
    return _varprefix + s;
  }

  public static Map<String, String> genericizeMap(Map properties) {
    // convert an untyped Map to a generic map
    Map<String, String> m = new HashMap<String, String>();
    Iterator iterator = properties.keySet().iterator();
    while (iterator.hasNext()) {
      Object key = iterator.next();
      Object value = properties.get(key);
      if ((key instanceof String) && (value instanceof String)) {
        m.put((String) key, (String) value);
      }
    }
    return Collections.unmodifiableMap(m);
  }

  protected String _getStringProp(MessageContext msgCtxt, String name, String defaultValue) {
    String value = this.properties.get(name);
    if (value != null) value = value.trim();
    if (value == null || value.equals("")) {
      return defaultValue;
    }
    value = resolveVariableReferences(value, msgCtxt);
    if (value == null || value.equals("")) {
      value = defaultValue;
    }
    return value;
  }

  protected String _getRequiredString(MessageContext msgCtxt, String name)
      throws IllegalStateException {
    String value = _getStringProp(msgCtxt, name, null);
    if (value == null)
      throw new IllegalStateException(String.format("%s resolves to null or empty.", name));
    return value;
  }

  protected String _getOptionalString(MessageContext msgCtxt, String name) {
    return _getStringProp(msgCtxt, name, null);
  }

  private String resolveVariableReferences(String spec, MessageContext msgCtxt) {
    Matcher matcher = variableReferencePattern.matcher(spec);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(sb, "");
      sb.append(matcher.group(1));
      String ref = matcher.group(2);
      String[] parts = ref.split(":", 2);
      Object v = msgCtxt.getVariable(parts[0]);
      if (v != null) {
        sb.append((String) v);
      } else if (parts.length > 1) {
        sb.append(parts[1]);
      }
      sb.append(matcher.group(3));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  protected boolean getDebug(MessageContext msgCtxt) throws Exception {
    return _getBooleanProperty(msgCtxt, "debug", false);
  }

  protected boolean _getBooleanProperty(MessageContext msgCtxt, String propName, boolean defaultValue)
      throws Exception {
    String flag = this.properties.get(propName);
    if (flag != null) flag = flag.trim();
    if (flag == null || flag.equals("")) {
      return defaultValue;
    }
    flag = resolveVariableReferences(flag, msgCtxt);
    if (flag == null || flag.equals("")) {
      return defaultValue;
    }
    return flag.equalsIgnoreCase("true");
  }

  protected void setExceptionVariables(Exception exc1, MessageContext msgCtxt) {
    String error = exc1.toString().replaceAll("\n", " ");
    msgCtxt.setVariable(varName("exception"), error);
    Matcher matcher = commonErrorPattern.matcher(error);
    if (matcher.matches()) {
      msgCtxt.setVariable(varName("error"), matcher.group(2));
    } else {
      msgCtxt.setVariable(varName("error"), error);
    }
  }

  protected static String exceptionStackTrace(Throwable t) {
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }
}
