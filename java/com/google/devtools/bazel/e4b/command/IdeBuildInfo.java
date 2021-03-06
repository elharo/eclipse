// Copyright 2016 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.bazel.e4b.command;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * A parsed version of the JSON files returned by the application of the IDE build information
 * aspect.
 */
public final class IdeBuildInfo {

  private static final Joiner COMMA_JOINER = Joiner.on(",");

  /**
   * A structure containing the list of jar files generated by a target (interface, class and source
   * jars).
   */
  public static final class Jars {
    private final String ijar; // interface_jar
    private final String jar; // jar
    private final String srcjar; // source_jar

    Jars(JSONObject obj) {
      this.ijar = obj.has("interface_jar") ? obj.getString("interface_jar") : null;
      this.jar = obj.getString("jar");
      this.srcjar = obj.has("srcjar") ? obj.getString("srcjar") : null;
    }

    @Override
    public String toString() {
      StringBuffer builder = new  StringBuffer();
      builder.append("Jars(jar = ").append(jar);
      if (ijar != null) {
        builder.append(", ijar = ").append(ijar);
      }
      if (srcjar != null) {
        builder.append(", srcjar = ").append(srcjar);
      }
      return builder.append(")").toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(ijar, jar, srcjar);
    }

    public String getInterfaceJar() {
      return ijar;
    }

    public String getJar() {
      return jar;
    }

    public String getSrcJar() {
      return srcjar;
    }
  }

  private final String location; // build_file_artifact_location
  private final ImmutableList<String> deps; // dependencies
  private final String kind; // kind
  private final String label; // label

  private final ImmutableList<Jars> generatedJars; // generated_jars
  private final ImmutableList<Jars> jars; // jars
  private final ImmutableList<String> sources; // sources

  /**
   * Construct an {@link IdeBuildInfo} object from a {@link JSONObject}.
   */
  IdeBuildInfo(JSONObject object) {
    jars = jsonToJarArray(object.getJSONArray("jars"));
    generatedJars = jsonToJarArray(object.getJSONArray("generated_jars"));
    location = object.getString("build_file_artifact_location");
    kind = object.getString("kind");
    label = object.getString("label");
    this.deps = jsonToStringArray(object.getJSONArray("dependencies"));
    this.sources = jsonToStringArray(object.getJSONArray("sources"));
  }

  @Override
  public String toString() {
    StringBuffer builder = new StringBuffer();
    builder.append("IdeBuildInfo(\n");
    builder.append("  label = ").append(label).append(",\n");
    builder.append("  location = ").append(location).append(",\n");
    builder.append("  kind = ").append(kind).append(",\n");
    builder.append("  jars = [").append(COMMA_JOINER.join(jars)).append("],\n");
    builder.append("  generatedJars = [").append(COMMA_JOINER.join(generatedJars)).append("],\n");
    builder.append("  deps = [").append(COMMA_JOINER.join(deps)).append("],\n");
    builder.append("  sources = [").append(COMMA_JOINER.join(sources)).append("])");
    return builder.toString();
  }

  /**
   * Constructs a map of label -> {@link IdeBuildInfo} from a list of files, parsing each files into
   * a {@link JSONObject} and then converting that {@link JSONObject} to an {@link IdeBuildInfo}
   * object.
   */
  @VisibleForTesting
  public static ImmutableMap<String, IdeBuildInfo> getInfo(List<String> files)
      throws IOException, InterruptedException {
    ImmutableMap.Builder<String, IdeBuildInfo> infos = ImmutableMap.builder();
    for (String s : files) {
      if (!s.isEmpty()) {
        IdeBuildInfo buildInfo =
            new IdeBuildInfo(new JSONObject(new JSONTokener(new FileInputStream(s))));
        infos.put(buildInfo.label, buildInfo);
      }
    }
    return infos.build();
  }

  private ImmutableList<Jars> jsonToJarArray(JSONArray array) {
    ImmutableList.Builder<Jars> builder = ImmutableList.builder();
    for (Object o : array) {
      builder.add(new Jars((JSONObject) o));
    }
    return builder.build();
  }

  private ImmutableList<String> jsonToStringArray(JSONArray array) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (Object o : array) {
      builder.add(o.toString());
    }
    return builder.build();
  }

  /**
   * Location of the target (build file).
   */
  public String getLocation() {
    return location;
  }

  /**
   * List of dependencies of the target.
   */
  public List<String> getDeps() {
    return deps;
  }

  /**
   * Kind of the target (e.g., java_test or java_binary).
   */
  public String getKind() {
    return kind;
  }

  /**
   * Label of the target.
   */
  public String getLabel() {
    return label;
  }

  /**
   * List of jars generated by annotations processors when building this target.
   */
  public List<Jars> getGeneratedJars() {
    return generatedJars;
  }

  /**
   * List of jars generated by building this target.
   */
  public List<Jars> getJars() {
    return jars;
  }

  /**
   * List of sources consumed by this target.
   */
  public List<String> getSources() {
    return sources;
  }
}
