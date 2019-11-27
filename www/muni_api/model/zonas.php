<?php
/**
 * Created by PhpStorm.
 * User: jhonny
 * Date: 22/09/19
 * Time: 07:22 PM
 */

require_once '../datos/conexion.php';
class zonas  extends conexion
{
    private $id;
    private $nombre;

    /**
     * @return mixed
     */
    public function getId()
    {
        return $this->id;
    }

    /**
     * @param mixed $id
     */
    public function setId($id)
    {
        $this->id = $id;
    }

    /**
     * @return mixed
     */
    public function getNombre()
    {
        return $this->nombre;
    }

    /**
     * @param mixed $nombre
     */
    public function setNombre($nombre)
    {
        $this->nombre = $nombre;
    }

    public function zona_list()
    {
        try {

            $sql = "SELECT * from zona";
            $sentencia = $this->dblink->prepare($sql);
            $sentencia->execute();
            $resultado = $sentencia->fetchAll();
            return $resultado;

        } catch (Exception $ex) {
            throw $ex;
        }
    }

}