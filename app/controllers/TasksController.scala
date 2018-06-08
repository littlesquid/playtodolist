package controllers

/**
  * Created by lesquid on 8/6/2561.
  */

import javax.inject.Inject

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, BodyParsers, Call, Controller, Result}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.core.actors.Exceptions.PrimaryUnavailableException
import reactivemongo.api.commands.WriteResult
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import service.TaskService

class TasksController @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends
  Controller with MongoController with ReactiveMongoComponents {

  import model.TaskFields._

  def taskService = new TaskService(reactiveMongoApi)

  def list = Action.async {implicit request =>
    taskService.find()
      .map(tasks => Ok(Json.toJson(tasks.reverse)))
      .recover {case PrimaryUnavailableException => InternalServerError("Please install MongoDB")}
  }

  def like(id: String) = Action.async(BodyParsers.parse.json) { implicit request =>
    val value = (request.body \ Subject).as[Boolean]
    taskService.update(BSONDocument(Id -> BSONObjectID(id)), BSONDocument("$set" -> BSONDocument(Subject -> value)))
      .map(le => Ok(Json.obj("success" -> le.ok)))
  }

  def update(id: String) = Action.async(BodyParsers.parse.json) { implicit request =>
      taskService.update(request.body).map(le => Ok(Json.obj("success" -> le.ok)))
  }

  def delete(id: String) = Action.async {
    taskService.remove(BSONDocument(Id -> BSONObjectID(id)))
      .map(le => RedirectAfterPost(le, routes.Posts.list()))
  }

  private def RedirectAfterPost(result: WriteResult, call: Call): Result =
    if (result.inError) InternalServerError(result.toString)
    else Redirect(call)

  def add = Action.async(BodyParsers.parse.json) { implicit request =>
    val subject = (request.body \ Subject).as[String]
    val status = (request.body \ TaskStatus).as[String]
    taskService.save(BSONDocument(
      Subject -> subject,
      TaskStatus -> status
    )).map(le => Redirect(routes.TaskController.list()))
  }}
