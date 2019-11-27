package com.easywaste.app

import android.graphics.Color
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.google.android.gms.maps.SupportMapFragment
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.easywaste.app.Clases.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.android.synthetic.main.servicio_reciclador_aceptar.*
import org.json.JSONObject
import java.lang.Exception

import java.util.ArrayList


class ServicioRecicladorOperacionFragment : Fragment() {

    var loc : ClsLocalizacion? =null
    var txtTiempoEstimado : TextView? =null
    var tiempoEstimado:Int = 1
    var cardViewInfo : CardView? =null
    var SERVICIOID:Int = 0
    var SERVICIO_DIRECCION:ClsServicioDireccion? = null
    var SERVICIO:ClsServicio? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val acti = activity as AppCompatActivity
        loc = ClsLocalizacion(acti)
        val view = inflater.inflate(R.layout.servicio_reciclador_operaciones, container, false)
        val mapFragment: SupportMapFragment = childFragmentManager.findFragmentById(R.id.frg) as SupportMapFragment
        mapFragment.getMapAsync(loc)
        if(arguments!=null){
            SERVICIOID = arguments!!.getInt("servicio_id")
        }else{
            SERVICIOID = Prefs.pullServicioRecicladorId()
        }
        val activity = activity as MainActivity?

        cardViewInfo = view.findViewById(R.id.infoserviciomapa)
        cardViewInfo?.visibility = View.INVISIBLE
        buscarServicioRecicladorEstado()
        return view
    }
     fun agregarMarcadorProveedor(pos:LatLng){
         loc!!.agregarMarcador( loc!!.markerProveedor(pos))
     }
     fun dibujarRuta(origin:LatLng, dest:LatLng, txtTiempoEstimado:TextView?){
        val url = getDirectionsUrl(origin, dest)
        loc?.gmap!!.clear()
         loc?.agregarMarcador(loc!!.markerReciclador(origin))
         loc?.agregarMarcador(loc!!.markerProveedor(dest))

         val request : JsonObjectRequest = object : JsonObjectRequest(
                Method.GET, url,null,
                Response.Listener { response ->
                    var routes: List<List<java.util.HashMap<String, String>>>? = null

                    try {

                        val parser = DirectionsJSONParser()
                        routes = parser.parse(response)
                        if(txtTiempoEstimado!=null) txtTiempoEstimado.text = "Llega en "+ parser.parseTiempo()
                        tiempoEstimado = parser.tiempoMin

                        var points: ArrayList<LatLng>? = null
                        var lineOptions: PolylineOptions? = null

                        for (i in routes.indices) {
                            points = ArrayList()
                            lineOptions = PolylineOptions()

                            // Fetching i-th route
                            val path = routes.get(i)

                            // Fetching all the points in i-th route
                            for (j in path.indices) {
                                val point = path.get(j)
                                val lat = java.lang.Double.parseDouble(point.get("lat")!!)
                                val lng = java.lang.Double.parseDouble(point.get("lng")!!)
                                val position = LatLng(lat, lng)
                                points.add(position)
                            }

                            // Adding all the points in the route to LineOptions
                            lineOptions.addAll(points)
                            lineOptions.width(8f)
                            lineOptions.color(Color.RED)
                        }

                        loc?.gmap?.addPolyline(lineOptions)
                        cardViewInfo?.visibility = View.VISIBLE


                    } catch (e: Exception) {
                        Toast.makeText(context,  "Error de estimación", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }


                },
                Response.ErrorListener{
                    Toast.makeText(context,  "Error de conexión", Toast.LENGTH_LONG).show()
                }) {}


        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(request)
    }

     fun getDirectionsUrl( origin:LatLng, dest:LatLng):String{

        val str_origin = "origin="+origin.latitude+","+origin.longitude

        val str_dest = "destination="+dest.latitude+","+dest.longitude

        val key = "key=" + getString(R.string.api_google)

        val parameters = str_origin+"&"+str_dest+"&"+key

        val output = "json?language=es&"

        val url = "https://maps.googleapis.com/maps/api/directions/"+output+parameters;
        Log.e("error", url.toString())

        return url
    }


    fun buscarServicioRecicladorEstado(){
        var idservicio = 0
        if(Prefs.pullServicioRecicladorId() !=0){
            idservicio = Prefs.pullServicioRecicladorId()
        }else{
            idservicio = SERVICIOID
        }
        val params = HashMap<String,Any>()
        params["servicio_id"] =  idservicio
        val parameters = JSONObject(params as Map<String, Any>)

        val request : JsonObjectRequest = object : JsonObjectRequest(
            Method.POST, VAR.url("servicio_info"),parameters,
            Response.Listener { response ->

                if(response!=null){
                    if(response.getInt("estado") == 200 ){
                        val datos =  response.getJSONObject("datos")
                        val proveedor = datos.getString("proveedor")
                        val latitud = datos.getDouble("latitud")
                        val longitud = datos.getDouble("longitud")
                        val estado = datos.getString("estado")

                        SERVICIO = ClsServicio(idservicio, proveedor)
                        SERVICIO_DIRECCION = ClsServicioDireccion("" , LatLng(latitud,longitud))

                        if(estado == "Abierto"){
                            childFragmentManager.beginTransaction().replace(R.id.subfragmento, ServicioRecicladorAceptarFragment()).commit()
                        }else if (estado == "En Camino"||estado == "En Atencion"){
                            childFragmentManager.beginTransaction().replace(R.id.subfragmento, ServicioRecicladorLlegoFragment()).commit()
                        }else if(estado=="Finalizado"){
                            Prefs.putServicioRecicladorId(0)
                            val mainActivity:MainActivity = activity as MainActivity
                            mainActivity.cambiarFragment(ServicioRecicladorSolicitudesFragment())
                        }

                    }else{
                        AlertaMensaje.mostrarError(activity!!,response.getString("mensaje"))
                    }
                }

            },
            Response.ErrorListener{
                try {
                    val nr = it.networkResponse
                    val r = String(nr.data)
                    val response=  JSONObject(r)
                    Toast.makeText(context,  response.getString("mensaje"), Toast.LENGTH_LONG).show()
                }catch (ex: Exception){
                    ex.printStackTrace()
                    Toast.makeText(context,  "Error de conexión", Toast.LENGTH_LONG).show()
                    fragmentManager?.popBackStackImmediate()
                }

            }) {
            override fun getHeaders(): Map<String, String> {
                var params: MutableMap<String, String> =HashMap()
                params["TOKEN"] =  Prefs.pullToken()
                return params
            }
        }

        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(request)
    }

}