$times = @()

Write-Host "Démarrage du benchmark REST..."
Write-Host "100 appels POST /operations"
Write-Host ""

for ($i = 1; $i -le 100; $i++) {

    $start = Get-Date

    Invoke-RestMethod `
        -Uri "http://localhost:8080/operations" `
        -Method Post `
        -ContentType "application/json" `
        -Body '{"type":"ADD","a":4,"b":7}' | Out-Null

    $end = Get-Date

    $times += ($end - $start).TotalMilliseconds

    Write-Host "Appel $i / 100"
}

$total = ($times | Measure-Object -Sum).Sum
$average = ($times | Measure-Object -Average).Average
$minimum = ($times | Measure-Object -Minimum).Minimum
$maximum = ($times | Measure-Object -Maximum).Maximum

Write-Host ""
Write-Host "================================"
Write-Host "       MESURE REST - 100 APPELS"
Write-Host "================================"
Write-Host "Nombre d'appels : 100"
Write-Host "Temps total     : $total ms"
Write-Host "Temps moyen     : $average ms"
Write-Host "Temps minimum   : $minimum ms"
Write-Host "Temps maximum   : $maximum ms"
Write-Host "================================"