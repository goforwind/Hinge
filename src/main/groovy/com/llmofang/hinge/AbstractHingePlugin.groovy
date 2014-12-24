package com.llmofang.hinge

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile

public abstract class AbstractHingePlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    def hasApp = project.plugins.withType(AppPlugin)
    def hasLib = project.plugins.withType(LibraryPlugin)
    if (!hasApp && !hasLib) {
      throw new IllegalStateException("'android' or 'android-library' plugin required.")
    }

    def extension = getExtension()
    def pluginExtension = getPluginExtension()
    if (extension && pluginExtension) {
      project.extensions.create(extension, pluginExtension)
    }

    final def log = project.logger
    final String LOG_TAG = this.getClass().getName()

    final def variants
    if (hasApp) {
      variants = project.android.applicationVariants
    } else {
      variants = project.android.libraryVariants
    }

    configure(project)

    variants.all { variant ->
      if (skipVariant(variant)) {
        return;
      }
      log.debug(LOG_TAG, "Transforming classes in variant '${variant.name}'.")

      JavaCompile javaCompile = variant.javaCompile
      FileCollection classpathFileCollection = project.files(project.android.bootClasspath)
      classpathFileCollection += javaCompile.classpath
      log.debug("Myxx", classpathFileCollection.toString());

      for(IClassTransformer transformer : getTransformers(project) ) {
        String transformerClassName = transformer.getClass().getSimpleName()
        String transformationDir = "${project.buildDir}/intermediates/transformations/transform${transformerClassName}${variant.name.capitalize()}"
        log.debug("Myxx", "transformerClassName: " + transformerClassName);
        log.debug("Myxx", "transformationDir: " + transformationDir);

        def transformTask = "transform${transformerClassName}${variant.name.capitalize()}"
        project.task(transformTask, type: TransformationTask) {
          description = "Transform a file using ${transformerClassName}"
          destinationDir = project.file(transformationDir)
          from ("${javaCompile.destinationDir.path}")
          transformation = transformer
          classpath = classpathFileCollection
          outputs.upToDateWhen {
            false
          }
          eachFile {
            log.debug(LOG_TAG, "Transformed:" + it.path)
          }
        }

        project.tasks.getByName(transformTask).mustRunAfter(javaCompile)
        def copyTransformedTask = "copyTransformed${transformerClassName}${variant.name.capitalize()}"
        project.task(copyTransformedTask, type: Copy) {
          description = "Copy transformed file to build dir for ${transformerClassName}"
          from (transformationDir)
          into ("${javaCompile.destinationDir.path}")
          outputs.upToDateWhen {
            false
          }
          eachFile {
            log.debug(LOG_TAG, "Copied into build:" + it.path)
          }
        }
        project.tasks.getByName(copyTransformedTask).mustRunAfter(project.tasks.getByName(transformTask))
        log.debug(LOG_TAG, "Transformation installed after compile")
        variant.assemble.dependsOn(transformTask, copyTransformedTask)
        if (!hasLib) {
          variant.install?.dependsOn(transformTask, copyTransformedTask)
        }

        //support for robolectric plugin TODO add support for standard tests
        String testTaskName = "compileTest${variant.name.capitalize()}Java"
        project.tasks.whenTaskAdded { task ->
          if(task.name.equals(testTaskName)) {
            log.debug(LOG_TAG, "test task ${task.name} detected and configured for transformer ${transformerClassName}")
            project.tasks.getByName(testTaskName).dependsOn(transformTask, copyTransformedTask)
          }
        }
      }
    }
  }

  /**
   * Hook to configure the project under build.
   * Can be used to add other extensions, plugins, etc.
   * @param project the project under build.
   */
  protected void configure(Project project) {
  }

  /**
   * @return the name of the class of the plugin extension associated to the project's extension.
   * Can be null, then no extension is created.
   * @see #getExtension()
   */
  protected abstract Class getPluginExtension()

  /**
   * @return the extension of the project that this plugin can create.
   * It will be associated to the plugin extension.
   * Can be null, then no extension is created.
   * @see #getPluginExtension()
   */
  protected abstract String getExtension()

  /**
   * A list of transformer instances to be used during build.
   * @param project the project under build.
   */
  public abstract IClassTransformer[] getTransformers(Project project)

  /**
   * Can be overridden to skip variants.
   * @param variant the variant to skip or not.
   * @return true to skip the variant. Default is false. No variant skipped.
   */
  public boolean skipVariant(def variant) {
    return false;
  }
}
