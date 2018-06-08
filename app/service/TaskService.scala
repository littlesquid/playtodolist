package service

import backend.TaskRepo
import play.api.libs.json.{JsObject, JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocument, BSONObjectID}

import scala.concurrent.{ExecutionContext, Future}
/**
  * Created by lesquid on 8/6/2561.
  */
trait TaskServiceImpl {
  def find()(implicit ec: ExecutionContext): Future[List[JsObject]]
  def update(selector: BSONDocument,update:BSONDocument)(implicit ec:ExecutionContext):Future[WriteResult]
  def remove(document: BSONDocument)(implicit ec:ExecutionContext): Future[WriteResult]
  def save(document:BSONDocument)(implicit  ec:ExecutionContext): Future[WriteResult]
}

class TaskService(reactiveMongoApi: ReactiveMongoApi)extends TaskRepo{

  import model.TaskFields._
  import play.modules.reactivemongo.json._

  protected def collection = reactiveMongoApi.db.collection[JSONCollection]("tasks")

  def find()(implicit ec:ExecutionContext): Future[List[JsObject]] =
    collection.find(Json.obj()).cursor[JsObject](ReadPreference.Primary).collect[List]()

  def save(document: BSONDocument)(implicit ec: ExecutionContext):Future[WriteResult]=
    collection.update(BSONDocument("_id" -> document.get("_id").getOrElse(BSONObjectID.generate)),document,upsert = true)

  def update(selector: BSONDocument,update:BSONDocument)(implicit ec:ExecutionContext):Future[WriteResult] =
    collection.update(selector,update)

  def update(bd: JsValue)(implicit ec:ExecutionContext):Future[WriteResult] ={
    val value = (bd \ Subject).as[String]
    val key = BSONDocument(Id -> BSONDocument(Id -> value))
    val subject = (
      BSONDocument("$set" -> BSONDocument(Subject -> value)),
      BSONDocument("$set" -> BSONDocument(TaskStatus -> value))
    )
    collection.update(key,subject)
  }

  def remove(document: BSONDocument)(implicit ec: ExecutionContext): Future[WriteResult] = collection.remove(document)


}
