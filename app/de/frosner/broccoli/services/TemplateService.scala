package de.frosner.broccoli.services

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

import de.frosner.broccoli.conf
import de.frosner.broccoli.models.{ParameterInfo, Template}
import play.Logger
import play.api.Configuration
import play.api.libs.json.JsObject
import play.libs.Json

import scala.collection.JavaConversions
import scala.io.Source
import scala.util.Try
import scala.util.parsing.json.JSON

@Singleton
class TemplateService @Inject() (configuration: Configuration) {

  private val templatesDirectoryPath = configuration.getString(conf.TEMPLATES_DIR_KEY).getOrElse(conf.TEMPLATES_DIR_DEFAULT)

  val templates: Seq[Template] = {
    val templatesDirectory = new File(templatesDirectoryPath)
    Logger.info(s"Looking for templates in $templatesDirectoryPath")
    val templateDirectories = if (templatesDirectory.exists && templatesDirectory.isDirectory) {
      templatesDirectory.listFiles.filter(_.isDirectory).toSeq
    } else {
      Logger.error(s"Templates directory $templatesDirectoryPath is not a directory")
      Seq.empty
    }
    Logger.info(s"Found ${templateDirectories.size} template directories: ${templateDirectories.mkString(", ")}")

    val templates = templateDirectories.flatMap(templateDirectory => {
      val tryTemplate = Try {
        val templateFile = new File(templateDirectory, "template.json")
        val templateFileContent = Source.fromFile(templateFile).getLines.mkString("\n")
        val templateId = templateDirectory.getName
        val metaFile = new File(templateDirectory, "meta.json")
        val metaFileContent = Source.fromFile(metaFile).getLines.mkString("\n")
        val metaInformation = Json.parse(metaFileContent)
        val description = if (metaInformation.has("description"))
          metaInformation.get("description").asText
        else
          s"$templateId template"
        val parameterInfos: Map[String, ParameterInfo] = if (metaInformation.has("parameters")) {
          val fields = JavaConversions.asScalaIterator(metaInformation.get("parameters").fields()).toIterable
          fields.map { entry =>
            val name = entry.getKey
            val entryValue = entry.getValue
            val default = if (entryValue.has("default")) Some(entryValue.get("default").asText) else None
            (name, ParameterInfo(name, default))
          }.toMap
        } else {
          Map.empty
        }
        Template(templateId, templateFileContent, description, parameterInfos)
      }
      tryTemplate.failed.map(throwable => Logger.warn(s"Parsing template '$templateDirectory' failed: $throwable"))
      tryTemplate.toOption
    })
    Logger.info(s"Successfully parsed ${templates.size} templates: ${templates.map(_.id).mkString(", ")}")
    templates.sortBy(_.id)
  }

  def template(id: String): Option[Template] = templates.find(_.id == id)

}
