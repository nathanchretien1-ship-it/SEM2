<?php
// auth.php
header("Content-Type: application/json");
ini_set('display_errors', 1);
error_reporting(E_ALL);

require 'db.php';

// Gestion du GET pour tester dans le navigateur
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    echo json_encode(["message" => "API Auth (ProjetSEM) fonctionnelle."]);
    exit;
}

$input = json_decode(file_get_contents('php://input'), true);
$action = isset($input['action']) ? $input['action'] : '';
$options = [
    'salt' => "Groupe2tis2@sem2",
];
$pepper = "UnPepperTresSecret123!"; // Pepper à ajouter au mot de passe

if ($action === 'register') {
    $email = $input['email'] ?? '';
    $password = $input['password'] ?? '';
    $name = $input['name'] ?? 'Utilisateur';

    if (!$email || !$password) {
        echo json_encode(["success" => false, "message" => "Email ou mot de passe manquant"]);
        exit;
    }

    $passHash = password_hash($password . $pepper, PASSWORD_BCRYPT, $options);

    $token = bin2hex(random_bytes(32));
    $expiry = date('Y-m-d', strtotime('+1 year'));
    $created = date('Y-m-d');

    $sexe = "Autre";
    $birth = "2000-01-01";
    $weight = 0;
    $height = 0;

    try {
        $sql = "INSERT INTO users 
                (email, password, nameUsers, apiToken, tokenExpiry, createdAt, sexe, birthDate, weight, height) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        $stmt = $conn->prepare($sql);
        $stmt->bind_param("ssssssssdd", $email, $passHash, $name, $token, $expiry, $created, $sexe, $birth, $weight, $height);
        $stmt->execute();

        echo json_encode(["success" => true, "message" => "Compte créé avec succès"]);
        $stmt->close();
    }
    catch (Exception $e) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "Erreur : Email déjà utilisé ou données invalides."]);
    }
}

elseif ($action === 'login') {
    $email = $input['email'] ?? '';
    $password = $input['password'] ?? '';

    $stmt = $conn->prepare("SELECT idUser, password FROM users WHERE email = ?");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($row = $result->fetch_assoc()) {
        if (password_verify($password . $pepper, $row['password'])) {
            $token = bin2hex(random_bytes(32));
            $expiry = date('Y-m-d', strtotime('+1 year'));
            $userId = $row['idUser'];

            $update = $conn->prepare("UPDATE users SET apiToken = ?, tokenExpiry = ? WHERE idUser = ?");
            $update->bind_param("ssi", $token, $expiry, $userId);
            $update->execute();

            echo json_encode(["success" => true, "token" => $token]);
        }
        else {
            echo json_encode(["success" => false, "message" => "Mot de passe incorrect"]);
        }
    }
    else {
        echo json_encode(["success" => false, "message" => "Utilisateur inconnu"]);
    }
    $stmt->close();
}
else {
    echo json_encode(["success" => false, "message" => "Action inconnue"]);
}
$conn->close();
?>