/*
 ************************************************************************************
 * Copyright (C) 2012-2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.openbravo.client.kernel.BaseComponent;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.StaticResourceComponent;
import org.openbravo.model.ad.module.Module;

/**
 * @author mtaal, ral
 *
 */
public class MobileStaticResourceComponent extends BaseComponent {
  private static final Logger log = Logger.getLogger(MobileStaticResourceComponent.class);

  private static final String GEN_TARGET_LOCATION = "web/js/gen";

  // generatedJavascriptFilenames does not need to be synchronized because its used in a
  // synchronized method. If this variable is accesed out of the synchronized scope, it should be
  // synchronized
  private static HashMap<String, String> generatedJavascriptFilenames = new HashMap<String, String>();

  // remember the last moduleVersion md5
  private static String modulesVersionMd5 = null;

  /**
   * generates a new javascript file if the conditions are met: when tomcat starts, or in
   * development, or when some module have changed version, or if the file didn't exists. This will
   * not happen even if any of the javascript files has changed its code.
   *
   * synchronized because its quite heavy to do and race conditions would be nasty
   */
  private static synchronized String generateStaticResources(final StaticResourceComponent src,
      final String realPath, final String appName) {

    // verify if the moduleVersion has changed since the last tomcat start
    final String currentModulesVersionMd5 = getModulesVersionMd5();
    final boolean isModuleVersionChange = currentModulesVersionMd5 == modulesVersionMd5;

    // retrieve the previous generatedJavascriptFilename if exists
    final String existingJavascriptFilename = generatedJavascriptFilenames.get(appName);

    // explicitely calling the io subsystem gives tomcat the ability to not fail in the first
    // request if that request was made when tomcat just finished to started up. I have no idea
    // why and could be wrong, could change depending on tomcat versions, could be etc.
    // this is required even if the fileName is null
    final boolean isFileExists = new File(realPath + "/"
        + existingJavascriptFilename + ".js").exists();

    /*
     * set forceGeneration to 'true' to force the generation of a new javascript if the javascript
     * content changed. useful for development if no module is 'in development'
     */
    final boolean forceGeneration = false;

    // decide if a new javascript file must be generated. See conditions above
    if (existingJavascriptFilename == null //
        || isDevelopment() //
        || isModuleVersionChange //
        || !isFileExists //
        || forceGeneration) {

      // staticResourceComponent creates the directory structure
      final String generatedJavascriptMd5 = src.getStaticResourceFileName();

      final File newGeneratedFile = new File(realPath + "/" + generatedJavascriptMd5 + ".js");

      // get the modules version md5
      modulesVersionMd5 = currentModulesVersionMd5;

      // generate the definitive filename
      final String newGeneratedJavascriptFilename = DigestUtils.md5Hex(generatedJavascriptMd5
          + modulesVersionMd5)
          + "_" + appName;

      // if the filename has changed, use it. otherwise, delete the generated javascript file
      if (newGeneratedJavascriptFilename.equals(existingJavascriptFilename) && isFileExists) {
        // delete the generated file, as we don't need it and we don't want to use it because the
        // timestamp would be updated and, thus, the manifest would be invalidated
        newGeneratedFile.delete();
      } else {
        // rename the staticResouceComponent generated file to the definitive filename
        newGeneratedFile
        .renameTo(new File(realPath + "/" + newGeneratedJavascriptFilename + ".js"));
        // remember the filename
        generatedJavascriptFilenames.put(appName, newGeneratedJavascriptFilename);
        log.info("A new javascript file have been created: "
            + generatedJavascriptFilenames.get(appName));
      }

      // delete all other appName generated files, as no terminal should use them anymore
      if (new File(realPath).exists()) {
        for (final File file : new File(realPath + "/").listFiles()) {
          if (file.getName().contains(appName)
              && !file.getName().contains(generatedJavascriptFilenames.get(appName))) {
            file.delete();
          }
        }
      }
    }

    return generatedJavascriptFilenames.get(appName);
  }

  private static String getModulesVersionMd5() {
    final StringBuffer versionString = new StringBuffer();
    final List<Module> modules = KernelUtils.getInstance().getModulesOrderedByDependency();
    for (final Module module : modules) {
      versionString.append(module.getVersion());
    }
    return DigestUtils.md5Hex(versionString.toString());
  }

  @Inject
  private StaticResourceComponent staticResourceComponent;

  @Override
  public String generate() {
    return "enyo.depends('" + getContextUrl() + GEN_TARGET_LOCATION + "/"
        + getGeneratedJavascriptFilename() + ".js" + "');";
  }

  /**
   * the exposed method to get the generated javascript filename if the file does not exists, the
   * file is created before this method returns. this can take several milliseconds
   *
   * @param appName
   */
  public String getGeneratedJavascriptFilename() {
    staticResourceComponent.setParameters(getParameters());
    final ServletContext context = (ServletContext) getParameters().get(
        KernelConstants.SERVLET_CONTEXT);
    final String appName = getParameters().containsKey("_appName")
        ? (String) getParameters().get("_appName")
            : MobileCoreConstants.RETAIL_CORE;
        return generateStaticResources(staticResourceComponent,
            context.getRealPath(GEN_TARGET_LOCATION) + "/", appName);
  }

  @Override
  public Object getData() {
    return null;
  }

  public static boolean isDevelopment() {
    for (final Module module : KernelUtils.getInstance().getModulesOrderedByDependency()) {
      if (module.isInDevelopment()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean bypassAuthentication() {
    return true;
  }

  @Override
  public String getETag() {
    // prevent the browser to cache this class call
    // i.e.: "../../org.openbravo.mobile.core/OBMOBC_Main/StaticResources?_appName=xxxx"
    return String.valueOf(System.currentTimeMillis());
  }
}