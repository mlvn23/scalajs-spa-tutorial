package services

import java.util.{UUID, Date}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import spatutorial.shared._
import java.nio.ByteBuffer

import boopickle.Default._

object ApiServer {

  val apiService = new ApiService()

  object Router extends autowire.Server[ByteBuffer, Pickler, Pickler] {
    override def read[R: Pickler](p: ByteBuffer) = Unpickle[R].fromBytes(p)
    override def write[R: Pickler](r: R) = Pickle.intoBytes(r)
  }

  /*
   * NOTE:
   *
   * SBT appears to have an issue in not incrementally compiling macros when its
   * dependents changes. So in this case, the Router is dependent on Api trait,
   * and if that trait changes, all invocation to the Router needs to be recompiled.
   * The original Router invocation was in Application, but Application will probably
   * not change when you change the Api service. As a work-around, move it here because
   * if you change the Api trait, you will need to change the ApiService implementation,
   * so this entire file (including the router invocation) would have to be recompiled.
   */
  def route(path: String, b: Array[Byte]): Future[Array[Byte]] = {
    Router.route[Api](apiService)(
      autowire.Core.Request(path.split("/"), Unpickle[Map[String, ByteBuffer]].fromBytes(ByteBuffer.wrap(b)))
    ).map(buffer => {
      val data = Array.ofDim[Byte](buffer.remaining())
      buffer.get(data)
      data
    })
  }

  class ApiService extends Api {
    var todos = Seq(
      TodoItem("41424344-4546-4748-494a-4b4c4d4e4f50", 0x61626364, "Wear shirt that says “Life”. Hand out lemons on street corner.", TodoLow, false),
      TodoItem("2", 0x61626364, "Make vanilla pudding. Put in mayo jar. Eat in public.", TodoNormal, false),
      TodoItem("3", 0x61626364, "Walk away slowly from an explosion without looking back.", TodoHigh, false),
      TodoItem("4", 0x61626364, "Sneeze in front of the pope. Get blessed.", TodoNormal, true)
    )

    override def motd(name: String): String = s"Welcome to SPA, $name! Time is now ${new Date}"

    override def getTodos(): Seq[TodoItem] = {
      // provide some fake Todos
      println(s"Sending ${todos.size} Todo items")
      todos
    }

    // update a Todo
    override def updateTodo(item: TodoItem): Seq[TodoItem] = {
      // TODO, update database etc :)
      if (todos.exists(_.id == item.id)) {
        todos = todos.collect {
          case i if i.id == item.id => item
          case i => i
        }
        println(s"Todo item was updated: $item")
      } else {
        // add a new item
        val newItem = item.copy(id = UUID.randomUUID().toString)
        todos :+= newItem
        println(s"Todo item was added: $newItem")
      }
      todos
    }

    // delete a Todo
    override def deleteTodo(itemId: String): Seq[TodoItem] = {
      println(s"Deleting item with id = $itemId")
      todos = todos.filterNot(_.id == itemId)
      todos
    }
  }

}