package com.easywaste.app

import android.content.Intent
import android.os.Bundle

import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.MenuItem
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.libraries.places.api.Places
import org.angmarch.views.NiceSpinner
import java.util.Arrays.asList
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.Log
import android.view.View
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.easywaste.app.Clases.*
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject
import java.lang.Exception
import java.util.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    var toggle:ActionBarDrawerToggle? = null
    var  drawerLayout: DrawerLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Prefs.getInstance(applicationContext,true)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        /*
        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        */
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val txtNombre = headerView.findViewById<TextView>(R.id.nombre)
        val txtDNI = headerView.findViewById<TextView>(R.id.dni)
        val txtCelular = headerView.findViewById<TextView>(R.id.celular)
        val txtRol = headerView.findViewById<TextView>(R.id.rol)
        txtNombre.text = Prefs.pullString(Prefs.PERSONA)
        txtDNI.text = Prefs.pullString(Prefs.DNI)
        txtCelular.text = Prefs.pullString(Prefs.CELULAR)
        txtRol.text = Prefs.pullString(Prefs.ROL)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )


        if (!Places.isInitialized()) {
            Places.initialize(this, getString(R.string.api_google))
        }

        drawerLayout!!.addDrawerListener(toggle!!)

        toggle!!.syncState()
        navView.setNavigationItemSelectedListener(this)
        val menu = navView.menu
        val rolid = Prefs.pullRolId()
        if(rolid==3){
            //PROVEEDOR
            menu.findItem(R.id.nav_servicio).setVisible(true)
        }else if(rolid==2){
            //RECICLADOR
            val estadoReciclador : NiceSpinner = headerView.findViewById(R.id.estado_reciclador)
            estadoReciclador.visibility = View.VISIBLE
            val dataset = listOf("Disponible", "No Disponible", "Ocupado")
            estadoReciclador.attachDataSource(dataset)
            estadoReciclador.setOnSpinnerItemSelectedListener { parent, view, position, id ->
                actualizarEstadoReciclador(parent,true)
            }
            var estado_reciclador = Prefs.pullInt(Prefs.RECICLADOR_ESTADO)
            if(estado_reciclador<1){ estado_reciclador = 1 }
            estadoReciclador.selectedIndex = estado_reciclador - 1
            actualizarEstadoReciclador(estadoReciclador)

            if(Prefs.pullServicioRecicladorId() !=0){
                val frag = ServicioRecicladorOperacionFragment()
                cambiarFragment(frag)
            }else{
                val frag = ServicioRecicladorSolicitudesFragment()
                cambiarFragment(frag)
            }
        }


    }

    fun actualizarEstadoReciclador(parent: NiceSpinner, update:Boolean = false){
        val  item = parent.getItemAtPosition( parent.selectedIndex)
        parent.setTextColor(resources.getColor(R.color.textWhite))
        var estado:Int= 0
        when(item) {
            "Disponible" -> {
                parent.setBackgroundColor(resources.getColor(R.color.primaryColor))
                estado = 1
            }
            "No Disponible" ->{
                parent.setBackgroundColor(resources.getColor(R.color.colorRed))
                estado = 2
            }

            "Ocupado" -> {
                parent.setBackgroundColor(resources.getColor(R.color.colorOrange))
                estado = 3
            }

        }
        if(update){
            val params = HashMap<String,Any>()
            params["id"] =  Prefs.pullId()
            params["status"] = estado

            val parameters = JSONObject(params as Map<String, Any>)

            val request : JsonObjectRequest = object : JsonObjectRequest(
                Method.POST, VAR.url("reciclador_status"),parameters,
                Response.Listener { response ->

                    if(response!=null){
                        if(response.getInt("estado") == 200 ){
                            Prefs.putInt(Prefs.RECICLADOR_ESTADO, estado)
                            Toast.makeText(applicationContext,  response.getString("mensaje"), Toast.LENGTH_LONG).show()
                        }else{
                            Prefs.putInt(Prefs.RECICLADOR_ESTADO, estado)

                            var estadoReciclador = Prefs.pullInt(Prefs.RECICLADOR_ESTADO)
                            if(estadoReciclador<1){ estadoReciclador = 1 }
                            actualizarEstadoReciclador(parent)
                            Toast.makeText(applicationContext,  response.getString("mensaje"), Toast.LENGTH_LONG).show()
                        }
                    }

                },
                Response.ErrorListener{
                    try {
                        val nr = it.networkResponse
                        val r = String(nr.data)
                        val response=  JSONObject(r)
                        Prefs.putInt(Prefs.RECICLADOR_ESTADO, estado)
                        var estadoReciclador = Prefs.pullInt(Prefs.RECICLADOR_ESTADO)
                        if(estadoReciclador<1){ estadoReciclador = 1 }
                        actualizarEstadoReciclador(parent)
                        Toast.makeText(applicationContext,  response.getString("mensaje"), Toast.LENGTH_LONG).show()
                    }catch (ex: Exception){
                        Log.e("error", ex.message)
                        ex.printStackTrace()
                        Toast.makeText(applicationContext,  "Error de conexión", Toast.LENGTH_LONG).show()
                    }

                }) {
                override fun getHeaders(): Map<String, String> {
                    var params: MutableMap<String, String> =HashMap()
                    params["TOKEN"] =  Prefs.pullToken()
                    return params
                }
            }

            val requestQueue = Volley.newRequestQueue(this)
            requestQueue.add(request)
        }

    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        //menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.



        when (item.itemId) {
            R.id.action_settings -> return true
            R.id.home  -> {
                supportFragmentManager.popBackStack()
                return true
            }


            else -> return super.onOptionsItemSelected(item)
        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {

            R.id.nav_cuenta -> {

            }
            R.id.nav_servicio -> {
                if(Prefs.pullServicioId() == 0){
                    val frag = ServicioProveedorRegistrarFragment()
                    cambiarFragment(frag)
                }else{
                    val frag = ServicioProveedorEspereFragment()
                    cambiarFragment(frag)
                }
            }
            R.id.nav_logout  -> {
                Prefs.destroy()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }

        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun cambiarFragment(frag:Fragment){
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmento, frag)
        fragmentTransaction.commit()
    }
    fun cambiarFragmentBackStack(frag:Fragment){
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmento, frag)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }
    fun habilitarNavDrawer(habilitado:Boolean){

        if (!habilitado) {
            toggle!!.isDrawerIndicatorEnabled = false
            drawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            toggle?.setToolbarNavigationClickListener { onBackPressed() }
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } else {
            drawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            toggle?.isDrawerIndicatorEnabled = true
            toggle?.toolbarNavigationClickListener = null
            toggle?.syncState()
        }
    }

    fun mostrarBotonMenu(){
        supportActionBar!!.show()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_menu)
    }

    fun mostrarBotonAtras(){
        supportActionBar!!.show()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_back)
    }

}
