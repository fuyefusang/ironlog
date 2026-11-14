param(
    [Parameter(Mandatory=$true)][string]$Action,
    [string]$Target,          # text substring for tap
    [int]$Index = 0,
    [string]$Name = "shot"
)

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$root = Split-Path -Parent $PSScriptRoot

function Dump-Ui {
    & $adb shell uiautomator dump /sdcard/ui.xml 2>$null | Out-Null
    & $adb pull /sdcard/ui.xml "$root\screenshots\ui.xml" 2>$null | Out-Null
    $xml = Get-Content "$root\screenshots\ui.xml" -Raw -Encoding UTF8
    return $xml
}

function Get-Nodes([string]$xml) {
    $out = @()
    foreach ($m in [regex]::Matches($xml, '<node [^>]*?/?>')) {
        $v = $m.Value
        $text = [regex]::Match($v, 'text="([^"]*)"').Groups[1].Value
        $bounds = [regex]::Match($v, 'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
        if (-not $bounds.Success) { continue }
        $out += [pscustomobject]@{
            Text = $text
            Xml  = [System.Net.WebUtility]::HtmlDecode($text)
            X1 = [int]$bounds.Groups[1].Value; Y1 = [int]$bounds.Groups[2].Value
            X2 = [int]$bounds.Groups[3].Value; Y2 = [int]$bounds.Groups[4].Value
            Clickable = ([regex]::Match($v, 'clickable="true"').Success)
        }
    }
    return $out
}

$xml = Dump-Ui
$nodes = Get-Nodes $xml

switch ($Action) {
    "tap" {
        $hits = @($nodes | Where-Object { $_.Xml -like "*$Target*" -and $_.X2 -gt $_.X1 })
        if ($hits.Count -eq 0) { Write-Output "NOTFOUND $Target"; exit 2 }
        $n = $hits[[Math]::Min($Index, $hits.Count - 1)]
        $cx = [int](($n.X1 + $n.X2) / 2); $cy = [int](($n.Y1 + $n.Y2) / 2)
        & $adb shell input tap $cx $cy
        Write-Output ("TAPPED '{0}' at {1},{2} (matched {3})" -f $n.Xml, $cx, $cy, $hits.Count)
    }
    "tapxy" {
        & $adb shell input tap ([int]$Target.Split(',')[0]) ([int]$Target.Split(',')[1])
        Write-Output "TAPPED $Target"
    }
    "shot" {
        & $adb shell screencap -p /sdcard/$Name.png
        & $adb pull /sdcard/$Name.png "$root\screenshots\$Name.png" 2>$null | Out-Null
        Write-Output "SHOT $Name"
    }
    "list" {
        $nodes | Where-Object { $_.Xml -ne "" -or $_.Clickable } |
            ForEach-Object { "{0,-40} click={1} [{2},{3}][{4},{5}]" -f $_.Xml, $_.Clickable, $_.X1, $_.Y1, $_.X2, $_.Y2 }
    }
    "text" {
        ($nodes | Where-Object { $_.Xml -ne "" } | ForEach-Object { $_.Xml }) -join " | "
    }
    "back" {
        & $adb shell input keyevent 4
        Write-Output "BACK"
    }
}
