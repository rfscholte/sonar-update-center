/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.updatecenter.common;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.updatecenter.common.exception.PluginNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class UpdateCenterDeserializerTest {

  @Test
  public void read_infos_from_froperties() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = UpdateCenterDeserializer.fromProperties(props);

      assertThat(center.getSonar().getVersions()).contains(Version.create("2.2"), Version.create("2.3"));
      assertThat(center.getSonar().getRelease(Version.create("2.2")).getDownloadUrl()).isEqualTo("http://dist.sonar.codehaus.org/sonar-2.2.zip");

      Plugin clirr = center.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getName()).isEqualTo("Clirr");
      assertThat(clirr.getDescription()).isEqualTo("Clirr Plugin");
      assertThat(clirr.getVersions()).contains(Version.create("1.0"), Version.create("1.1"));

      assertThat(clirr.getSourcesUrl()).isNull();
      assertThat(clirr.getDevelopers()).isEmpty();

      assertThat(clirr.getRelease(Version.create("1.0")).getDownloadUrl()).isEqualTo("http://dist.sonar-plugins.codehaus.org/clirr-1.0.jar");
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Test
  public void should_add_developers() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-developers.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = UpdateCenterDeserializer.fromProperties(props);

      Plugin clirr = center.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getDevelopers()).hasSize(3);
      assertThat(clirr.getDevelopers()).contains("Mike Haller", "Freddy Mallet", "Simon Brandhof");

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Test
  public void should_add_sources_url() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-scm.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter center = UpdateCenterDeserializer.fromProperties(props);

      Plugin clirr = center.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getSourcesUrl()).isEqualTo("scm:svn:https://svn.codehaus.org/sonar-plugins/tags/sonar-clirr-plugin-1.1");

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Test
  public void should_add_children() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-parent.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = UpdateCenterDeserializer.fromProperties(props);

      assertThat(pluginReferential.getUpdateCenterPluginReferential().getLastMasterReleasePlugins()).hasSize(1);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getName()).isEqualTo("Clirr");
      Release release = clirr.getRelease("1.1");
      assertThat(release.getChildren()).hasSize(1);
      assertThat(release.getChild("motionchart")).isNotNull();
      assertThat(release.getChild("motionchart").getParent().getKey()).isEqualTo("clirr");
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Test
  public void should_add_dependencies() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/updatecenter/common/UpdateCenterDeserializerTest/updates-with-requires-plugins.properties");
    try {
      Properties props = new Properties();
      props.load(input);
      UpdateCenter pluginReferential = UpdateCenterDeserializer.fromProperties(props);

      assertThat(pluginReferential.getUpdateCenterPluginReferential().getLastMasterReleasePlugins()).hasSize(3);

      Plugin clirr = pluginReferential.getUpdateCenterPluginReferential().findPlugin("clirr");
      assertThat(clirr.getName()).isEqualTo("Clirr");
      List<Release> requiredReleases = newArrayList(clirr.getRelease(Version.create("1.1")).getOutgoingDependencies());
      assertThat(requiredReleases).hasSize(2);
      assertThat(requiredReleases.get(0).getArtifact().getKey()).isEqualTo("foo");
      assertThat(requiredReleases.get(0).getVersion().getName()).isEqualTo("1.0");
      assertThat(requiredReleases.get(1).getArtifact().getKey()).isEqualTo("bar");
      assertThat(requiredReleases.get(1).getVersion().getName()).isEqualTo("1.1");
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Test(expected = PluginNotFoundException.class)
  public void should_throw_exception_when_parent_is_missing(){
    Properties props = new Properties();
    props.put("plugins", "foo");
    props.put("foo.name", "Foo");
    props.put("foo.versions", "1.1");
    props.put("foo.1.1.parent", "bar");
    UpdateCenterDeserializer.fromProperties(props);
  }
}
