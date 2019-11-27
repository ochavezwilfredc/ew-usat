package com.easywaste.app

import android.graphics.Color
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject
import org.w3c.dom.Text
import java.lang.Exception

import java.util.ArrayList


class ServicioProveedorEnCaminoFragment : Fragment() {

    var loc : ClsLocalizacion? =null
    var txtTiempoEstimado : TextView? =null
    var cardViewInfo : CardView? =null
    var SERVICIO_DIRECCION:ClsServicioDireccion? = null
    var SERVICIO:ClsServicio? = null
    var txtRecicladorDni:TextView? = null
    var txtReciclador:TextView? = null
    var OK = true
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val acti = activity as AppCompatActivity
        val view = inflater.inflate(R.layout.servicio_proveedor_encaminor, container, false)
        txtReciclador = view.findViewById(R.id.reciclador)
        txtRecicladorDni= view.findViewById(R.id.dni)
        txtTiempoEstimado = view.findViewById(R.id.tiempoestimado)
        cardViewInfo = view.findViewById(R.id.infoserviciomapa)
        cardViewInfo?.visibility = View.INVISIBLE

        val btnCancelar:Button = view.findViewById(R.id.btnCancelar)

        btnCancelar.setOnClickListener {

        }

        Toast.makeText(context,  "Espere ...", Toast.LENGTH_LONG).show()

        buscarServicioProveedorEstado()

        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                if(OK){
                    try {
                        buscarServicioProveedorEstado()
                        mainHandler.postDelayed(this, 3000)
                    }catch (ex:Exception){

                    }
                }

            }
        })

        return view
    }

    fun buscarServicioProveedorEstado(){
        val params = HashMap<String,Any>()
        params["servicio_id"] =  Prefs.pullServicioId()
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
                        val reciclador = datos.getString("reciclador")
                        val reciclador_dni = datos.getString("reciclador_dni")
                        val tiempo_aproximado = datos.getInt("tiempo_aprox_atencion")
                        SERVICIO = ClsServicio(Prefs.pullServicioId(), proveedor)
                        SERVICIO_DIRECCION = ClsServicioDireccion("" , LatLng(latitud,longitud))

                        if (estado == "En Camino"){
                            txtRecicladorDni?.text =reciclador_dni
                            txtReciclador?.text = reciclador
                            txtTiempoEstimado?.text= "Llega en $tiempo_aproximado minutos"
                            cardViewInfo?.visibility = View.VISIBLE
                        }else if(estado == "Finalizado"){

                            try {
                                OK = false
                                val mainActivity:MainActivity = activity as MainActivity
                                mainActivity.cambiarFragment(ServicioProveedorFinalizadoFragment())
                            }catch (ex:Exception){
                                //OK = false
                            }
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