<?php
header("Content-Type: application/json");
ini_set('display_errors', 1);
error_reporting(E_ALL);

require 'db.php';

function getBearerToken() {
    $headers = null;
    if (isset($_SERVER['Authorization'])) $headers = trim($_SERVER["Authorization"]);
    else if (isset($_SERVER['HTTP_AUTHORIZATION'])) $headers = trim($_SERVER["HTTP_AUTHORIZATION"]);
    elseif (function_exists('apache_request_headers')) {
        $req = apache_request_headers();
        if (isset($req['Authorization'])) $headers = trim($req['Authorization']);
    }
    if (!empty($headers) && preg_match('/Bearer\s(\S+)/', $headers, $matches)) return $matches[1];
    if (isset($_GET['token'])) return $_GET['token'];
    return null;
}

$token = getBearerToken();

if (!$token) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token manquant"]);
    exit;
}

$stmt = $conn->prepare("SELECT idUser, idDoctor FROM users WHERE apiToken = ?");
$stmt->bind_param("s", $token);
$stmt->execute();
$user = $stmt->get_result()->fetch_assoc();

if (!$user) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token invalide"]);
    exit;
}

$user_id = $user['idUser'];
$doctor_id = $user['idDoctor']; 

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $type_filter = isset($_GET['type']) ? $_GET['type'] : null;

    $sql = "SELECT deviceType, bpm, spo2, temperature, measureDate, measureTime 
            FROM measures 
            WHERE idUser = ? ";
    
    $paramsTypes = "i";
    $paramsValues = [$user_id];

    if ($type_filter) {
        $sql .= "AND deviceType = ? ";
        $paramsTypes .= "s";
        $paramsValues[] = $type_filter;
    }
    
    $sql .= "ORDER BY measureDate DESC, measureTime DESC LIMIT 50";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param($paramsTypes, ...$paramsValues);
    
    if ($stmt->execute()) {
        $result = $stmt->get_result();
        $data = [];
        while ($row = $result->fetch_assoc()) {
            $data[] = $row;
        }
        echo json_encode(["success" => true, "data" => $data]);
    } else {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Erreur SQL"]);
    }
}

elseif ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true);
    
    $device = isset($input['device_type']) ? $input['device_type'] : '';
    $bpm = isset($input['bpm']) ? $input['bpm'] : null;
    $spo2 = isset($input['spo2']) ? $input['spo2'] : null;
    $temp = isset($input['temperature']) ? $input['temperature'] : null;

    if (!$device) {
        echo json_encode(["success" => false, "message" => "Type appareil manquant"]);
        exit;
    }

    $sql = "INSERT INTO measures (idUser, idDoctor, deviceType, bpm, spo2, temperature, measureDate, measureTime) 
            VALUES (?, ?, ?, ?, ?, ?, CURDATE(), CURTIME())";
            
    $stmt = $conn->prepare($sql);
    
    $stmt->bind_param("iisddd", $user_id, $doctor_id, $device, $bpm, $spo2, $temp);

    if ($stmt->execute()) {
        echo json_encode(["success" => true, "message" => "Mesure enregistrée"]);
    } else {
        echo json_encode(["success" => false, "message" => "Erreur enregistrement: " . $stmt->error]);
    }
}
?>