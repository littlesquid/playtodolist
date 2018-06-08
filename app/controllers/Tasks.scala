package controllers

/**
  * Created by lesquid on 8/6/2561.
  */

import javax.inject.Inject

import backend.TaskMongoRepo
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, BodyParsers, Call, Controller, Result}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.core.actors.Exceptions.PrimaryUnavailableException
import reactivemongo.api.commands.WriteResult
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

class Tasks @Inject() (val reactiveMongoApi: ReactiveMongoApi)extends Controller with MongoController with ReactiveMongoComponents {
  import controllers.TaskFields._

  def taskRepo: TaskMongoRepo = new backend.TaskMongoRepo(reactiveMongoApi)

  def list = Action.async{
    implicit request=>
      taskRepo.find()
        .map(tasks => Ok(Json.toJson(tasks.reverse)))
          .recover{
            case PrimaryUnavailableException => InternalServerError("please install mongodb")
          }
  }

  def add = Action.async(BodyParsers.parse.json){
    implicit request =>
      val subject = (request.body \Subject).as[String]
      val status = (request.body \TaskState).as[String]
      taskRepo.save(BSONDocument(
          Subject -> subject,
        TaskState -> status
        )).map(le => Redirect(routes.TaskController.list())
      )
  }

  private def RedirectAfterPost(result: WriteResult,call:Call):Result=
    if(result.inError) InternalServerError(result.toString)
    else Redirect(call)
}

object TaskFields{
  val Id = "_id"
  val Subject = "subject"
  val TaskState = "status"
}
