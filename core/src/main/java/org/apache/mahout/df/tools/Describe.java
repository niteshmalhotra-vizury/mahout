/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.df.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.df.data.DataLoader;
import org.apache.mahout.df.data.Dataset;
import org.apache.mahout.df.data.DescriptorException;
import org.apache.mahout.df.data.DescriptorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a file descriptor for a given dataset
 */
public class Describe {
  
  private static final Logger log = LoggerFactory.getLogger(Describe.class);
  
  private Describe() { }
  
  public static void main(String[] args) throws IOException, DescriptorException {
    
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();
    
    Option pathOpt = obuilder.withLongName("path").withShortName("p").withRequired(true).withArgument(
      abuilder.withName("path").withMinimum(1).withMaximum(1).create()).withDescription("Data path").create();
    
    Option descriptorOpt = obuilder.withLongName("descriptor").withShortName("d").withRequired(true)
        .withArgument(abuilder.withName("descriptor").withMinimum(1).create()).withDescription(
          "data descriptor").create();
    
    Option descPathOpt = obuilder.withLongName("file").withShortName("f").withRequired(true).withArgument(
      abuilder.withName("file").withMinimum(1).withMaximum(1).create()).withDescription(
      "Path to generated descriptor file").create();
    
    Option helpOpt = obuilder.withLongName("help").withDescription("Print out help").withShortName("h")
        .create();
    
    Group group = gbuilder.withName("Options").withOption(pathOpt).withOption(descPathOpt).withOption(
      descriptorOpt).withOption(helpOpt).create();
    
    try {
      Parser parser = new Parser();
      parser.setGroup(group);
      CommandLine cmdLine = parser.parse(args);
      
      if (cmdLine.hasOption(helpOpt)) {
        CommandLineUtil.printHelp(group);
        return;
      }
      
      String dataPath = cmdLine.getValue(pathOpt).toString();
      String descPath = cmdLine.getValue(descPathOpt).toString();
      List<String> descriptor = convert(cmdLine.getValues(descriptorOpt));
      
      log.debug("Data path : {}", dataPath);
      log.debug("Descriptor path : {}", descPath);
      log.debug("Descriptor : {}", descriptor);
      
      runTool(dataPath, descriptor, descPath);
    } catch (OptionException e) {
      log.warn(e.toString(), e);
      CommandLineUtil.printHelp(group);
    }
  }
  
  private static void runTool(String dataPath, List<String> description, String filePath) throws DescriptorException,
                                                                                         IOException {
    log.info("Generating the descriptor...");
    String descriptor = DescriptorUtils.generateDescriptor(description);
    
    Path fPath = validateOutput(filePath);
    
    log.info("generating the dataset...");
    Dataset dataset = generateDataset(descriptor, dataPath);
    
    log.info("storing the dataset description");
    storeWritable(new Configuration(), fPath, dataset);
  }
  
  private static Dataset generateDataset(String descriptor, String dataPath) throws IOException,
                                                                            DescriptorException {
    Path path = new Path(dataPath);
    FileSystem fs = path.getFileSystem(new Configuration());
    
    return DataLoader.generateDataset(descriptor, fs, path);
  }
  
  private static Path validateOutput(String filePath) throws IOException {
    Path path = new Path(filePath);
    FileSystem fs = path.getFileSystem(new Configuration());
    if (fs.exists(path)) {
      throw new IllegalStateException("Descriptor's file already exists");
    }
    
    return path;
  }
  
  private static List<String> convert(List<?> values) {
    List<String> list = new ArrayList<String>(values.size());
    for (Object value : values) {
      list.add(value.toString());
    }
    return list;
  }
  
  private static void storeWritable(Configuration conf, Path path, Writable dataset) throws IOException {
    FileSystem fs = path.getFileSystem(conf);
    
    FSDataOutputStream out = fs.create(path);
    dataset.write(out);
    out.close();
  }
}
