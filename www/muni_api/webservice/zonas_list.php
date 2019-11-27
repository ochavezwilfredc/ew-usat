<?php
/**
 * Created by PhpStorm.
 * User: jhonny
 * Date: 22/09/19
 * Time: 07:22 PM
 */
header('Access-Control-Allow-Origin: *');

require_once '../model/zonas.php';
require_once '../util/funciones/Funciones.clase.php';
require_once 'tokenvalidar.php';


try {
    $obj = new zonas();
    $resultado = $obj->zona_list();

    if($resultado){
        Funciones::imprimeJSON(200, "",$resultado);
    }

} catch (Exception $exc) {

    Funciones::imprimeJSON(500, $exc->getMessage(), "");
}