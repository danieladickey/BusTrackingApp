package com.danieldickeyfinalproject.cvtdbuses

import android.util.Log
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

// xml parser tutorial found at:
// https://blog.mindorks.com/processing-and-parsing-xml-in-android
class CVTD {
    // count of buses being tracked
    var activeBuses: MutableList<MutableList<String>> = busParse()
    var count = 0


    // the function busParse returns a list of lists of strings
    // this list represents all buses being tracked
    // each sublist represents a single bus being tracked
    // each string in each sublist represents data about that bus ie: route name, latitude, longitude, etc.
    private fun busParse(): MutableList<MutableList<String>> {
        val busesBeingTracked: MutableList<MutableList<String>> = mutableListOf()
        try {
            val inputStream =
                downloadUrl("http://cvtd.info:8080/CVTDfeed/V200/XML/_System.php?key=MeNCLxxcFINYn4VKnNbU")
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(inputStream)
            val element = doc.documentElement
            element.normalize()
            val nList = doc.getElementsByTagName("Bus")
            // for each bus in the XML:
            for (i in 0 until nList.length) {
                val node = nList.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    val element2 = node as Element
                    // if the buss is active track it
                    if (getValue("BusNumber", element2) != "0" &&
                        getValue("latitude", element2) != "0.0"
                    ) {
                        // create a list representing a single tracked bus
                        val listOfBusInfo: MutableList<String> = mutableListOf()

                        // add the bus's pertinent info to the it's list
                        listOfBusInfo.add(getValue("RouteDescription", element2))
                        listOfBusInfo.add(getValue("latitude", element2))
                        listOfBusInfo.add(getValue("longitude", element2))
                        listOfBusInfo.add(getValue("RouteColor", element2))
                        // add this bus to the list of busesBeingTracked
                        busesBeingTracked.add(listOfBusInfo)
                        // log this bus's info to console
//                        Log.d("TAG", "busParse: busesBeingTracked: $listOfBusInfo")
                    }
                }
            }
            // log number of buses being tracked to console
//            Log.d("TAG", "Found GPS info for $numberOfBusesBeingTracked buses")
            // reset count
            count = busesBeingTracked.size
        } catch (e: Exception) {
            Log.d(
                "TAG",
                "busParse: Error in busParse function ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~!"
            )
            e.printStackTrace()
        }
        return busesBeingTracked
    }

    fun update() {
        activeBuses = busParse()
    }

    fun getBuses(): MutableList<MutableList<String>> {
        update()
        return activeBuses
    }

    // get info from XML
    private fun getValue(tag: String, element: Element): String {
        val nodeList = element.getElementsByTagName(tag).item(0).childNodes
        val node = nodeList.item(0)
        return node.nodeValue
    }

    // make a connection to URL
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream? {
        val url = URL(urlString)
        return (url.openConnection() as? HttpURLConnection)?.run {
            readTimeout = 10000
            connectTimeout = 15000
            requestMethod = "GET"
            doInput = true
            connect()
            inputStream
        }
    }
}