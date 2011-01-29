/*
 * Copyright (c) 2010 e.e d3si9n
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
 
package scalaxb.compiler.xsd

import scalaxb.compiler.{Module, Config, Snippet}
import scala.xml._

abstract class GenProtocol(val context: XsdContext) extends ContextProcessor {
  def generateProtocol(snippet: Snippet): Seq[Node] = {
    
    val name = makeTypeName("XMLProtocol")
    val scopeSchemas = context.schemas    
    def makeScopes(ss: List[SchemaDecl]): List[(Option[String], String)] = ss match {
      case x :: xs => 
        x.targetNamespace map { ns =>
          val prefix = makePrefix(x.targetNamespace, context)
          if (prefix == "") makeScopes(xs)
          else (Some(prefix), ns) :: makeScopes(xs)
        } getOrElse { makeScopes(xs) }
      case _ => Nil
    }

    val scopes0 = makeScopes(scopeSchemas.toList) ::: List((Some(XSI_PREFIX) -> XSI_URL))
    val scopes = config.primaryNamespace match {
      case Some(ns) =>
        val primaryPair = if (context.schemas forall {_.elementQualifiedDefault}) List((None, ns))
          else if (scopes0 exists {_._1 == ns}) Nil
          else List((Some("unq"), ns))
        (primaryPair ::: scopes0).distinct
      case _ => scopes0.distinct
    }

    val pkg = packageName(config.primaryNamespace, context)
    val packageString = pkg map { "package " + _ + newline } getOrElse{""}
    val packageImportString = pkg map { "import " + _ + "._" + newline } getOrElse {""}
    
    <source>// Generated by &lt;a href="http://scalaxb.org/"&gt;scalaxb&lt;/a&gt;.
{packageString}    
/**
usage:
import scalaxb._
import Scalaxb._
{packageImportString}import Default{name}._

val obj = fromXML[Foo](node)
val document = toXML[Foo](obj, "foo", defaultScope)
**/
trait {name} extends scalaxb.XMLStandardTypes {{
{snippet.implicitValue}  
}}

object { buildDefaultProtocolName(name) } extends { buildDefaultProtocolName(name) } with scalaxb.DefaultXMLStandardTypes {{
  import scalaxb.Scalaxb._
  val defaultScope = toScope({ if (scopes.isEmpty) "Nil: _*"
    else scopes.map(x => quote(x._1) + " -> " + quote(x._2)).mkString("," + newline + indent(2)) })  
}}

trait { buildDefaultProtocolName(name) } extends {name} {{
  import scalaxb.Scalaxb._

{snippet.companion}
}}</source>
  }
  
  def buildDefaultProtocolName(name: String): String = {
    config.classPrefix match {
      case Some(p) => p + "Default" + name.drop(p.length)
      case None => "Default" + name
    }
  }
  
//   def makeXMLProtocol(companions: Seq[Node], implicitValues: Seq[Node]) = {
//     val typeNames = context.typeNames(packageName(schema, context))
//     val name = typeNames(schema)
//     val imports = dependentSchemas map { sch =>
//         val pkg = packageName(sch, context)
//         val name = context.typeNames(pkg)(sch)
//         "import " + pkg.map(_ + ".").getOrElse("") + name + "._"
//       }
//     val traitSuperNames = "scalaxb.XMLStandardTypes" :: (dependentSchemas.toList map { sch =>
//         val pkg = packageName(sch, context)
//         pkg.map(_ + ".").getOrElse("") + context.typeNames(pkg)(sch)
//       })
//     val defaultTraitSuperNames =
//       List(buildDefaultProtocolName(name), "scalaxb.DefaultXMLStandardTypes") ::: (dependentSchemas.toList map { sch =>
//         val pkg = packageName(sch, context)
//         pkg.map(_ + ".").getOrElse("") + buildDefaultProtocolName(context.typeNames(pkg)(sch))
//       })
// 
//     def makeScopes(schemas: List[SchemaDecl]): List[(Option[String], String)] = schemas match {
//       case x :: xs => 
//         x.targetNamespace map { ns =>
//           val prefix = makePrefix(x.targetNamespace, context)
//           if (prefix == "") makeScopes(xs)
//           else (Some(prefix), ns) :: makeScopes(xs)
//         } getOrElse { makeScopes(xs) }
//       case _ => Nil
//     }
// 
//     val scopes = schema.targetNamespace map { ns =>
//       ((None, ns) :: makeScopes(schema :: dependentSchemas.toList) :::
//         List((Some(XSI_PREFIX) -> XSI_URL)) ).distinct
//     } getOrElse {makeScopes(dependentSchemas.toList).distinct}
// 
//     val packageImportString = packageName(schema, context) map { pkg =>
//       "import " + pkg + "._" + newline } getOrElse {""}
// 
//     <source>/** usage:
// import scalaxb._
// import Scalaxb._
// {packageImportString}import Default{name}._
// 
// val obj = fromXML[Foo](node)
// val document = toXML[Foo](obj, "foo", defaultScope)
// **/
// trait {name} extends { traitSuperNames.mkString(" with ") } {{
// {implicitValues}  
// }}
// 
// object { buildDefaultProtocolName(name) } extends { defaultTraitSuperNames.mkString(" with ") } {{
//   import scalaxb.Scalaxb._
//   val defaultScope = toScope({ if (scopes.isEmpty) "Nil: _*"
//     else scopes.map(x => quote(x._1) + " -> " + quote(x._2)).mkString("," + newline + indent(2)) })  
// }}
// 
// trait { buildDefaultProtocolName(name) } extends {name} {{
//   import scalaxb.Scalaxb._
//   private val targetNamespace: Option[String] = { quote(schema.targetNamespace) }
// 
// {companions}
// }}</source>
//   }
}