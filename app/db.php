<?php
$host = "localhost";
$user = "phpmyadmin";        
$pass = "passw*rdBDDGr2";
$dbname = "BaseDeTest";

$conn = new mysqli($host, $user, $pass, $dbname);

if ($conn->connect_error) {
    die(json_encode(["error" => "Echec connexion: " . $conn->connect_error]));
}
$conn->set_charset("utf8mb4");
?>