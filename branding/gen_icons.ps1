Add-Type -AssemblyName System.Drawing

$srcPath      = "D:\Android_Studio\ERP_360\branding\logo_missa.png"
$srcRoundPath = "D:\Android_Studio\ERP_360\branding\logo_missa_round.png"
$res          = "D:\Android_Studio\ERP_360\app\src\main\res"

$src = New-Object System.Drawing.Bitmap($srcPath)

# Optional dedicated round artwork (used for round launcher icons + adaptive foreground).
# If missing, the square artwork is used for everything.
$script:srcRound = $null
if (Test-Path $srcRoundPath) {
    $script:srcRound = New-Object System.Drawing.Bitmap($srcRoundPath)
    Write-Output "round artwork found: $srcRoundPath"
} else {
    Write-Output "round artwork NOT found ($srcRoundPath) - falling back to square artwork everywhere"
}

# --- Sample colors to decide clipping / background ---
$corner = $src.GetPixel(2, 2)
$top    = $src.GetPixel(627, 100)
$mid    = $src.GetPixel(627, 627)
Write-Output ("corner ARGB = {0},{1},{2},{3}" -f $corner.A, $corner.R, $corner.G, $corner.B)
Write-Output ("top    ARGB = {0},{1},{2},{3}" -f $top.A, $top.R, $top.G, $top.B)
Write-Output ("mid    ARGB = {0},{1},{2},{3}" -f $mid.A, $mid.R, $mid.G, $mid.B)

$cornerIsWhite = ($corner.A -gt 200 -and $corner.R -gt 240 -and $corner.G -gt 240 -and $corner.B -gt 240)
Write-Output ("cornerIsWhite = $cornerIsWhite")

function New-RoundedRectPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $d = 2 * $r
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $p.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $p.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $p.CloseFigure()
    return $p
}

function Save-Bitmap($bmp, $outPath) {
    $dir = Split-Path $outPath -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "written: $outPath"
}

# --- Legacy square launcher icon (rounded corners if source has white background) ---
function New-SquareIcon([int]$size, [string]$outPath) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.InterpolationMode = 'HighQualityBicubic'
    $g.PixelOffsetMode = 'HighQuality'
    if ($script:cornerIsWhite) {
        $path = New-RoundedRectPath 0 0 $size $size ($size * 0.20)
        $g.SetClip($path)
    }
    $g.DrawImage($script:src, 0, 0, $size, $size)
    $g.Dispose()
    Save-Bitmap $bmp $outPath
}

# --- Adaptive icon foreground: 108dp canvas, logo centered inside the 66dp safe zone ---
# With the dedicated round artwork, the disc is scaled to 72dp (the adaptive mask circle)
# and clipped to a circle so no white corners can ever show; the launcher background
# color (#0A54F2) fills the remaining ring seamlessly.
function New-ForegroundIcon([int]$size, [string]$outPath) {
    $useRound = [bool]$script:srcRound
    $img = if ($useRound) { $script:srcRound } else { $script:src }
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.InterpolationMode = 'HighQualityBicubic'
    $g.PixelOffsetMode = 'HighQuality'
    if ($useRound) {
        $logo = [int]($size * 72 / 108)   # match the adaptive mask circle diameter
        $off  = [int](($size - $logo) / 2)
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddEllipse($off, $off, $logo, $logo)
        $g.SetClip($path)
        $g.DrawImage($img, $off, $off, $logo, $logo)
    } else {
        $logo = [int]($size * 0.66)   # 66dp safe zone of the 108dp canvas
        $off  = [int](($size - $logo) / 2)
        if ($script:cornerIsWhite) {
            $path = New-RoundedRectPath $off $off $logo $logo ($logo * 0.20)
            $g.SetClip($path)
        }
        $g.DrawImage($img, $off, $off, $logo, $logo)
    }
    $g.Dispose()
    Save-Bitmap $bmp $outPath
}

# mdpi 48/108 | hdpi 72/162 | xhdpi 96/216 | xxhdpi 144/324 | xxxhdpi 192/432
$densities = [ordered]@{
    'mipmap-mdpi'    = 48
    'mipmap-hdpi'    = 72
    'mipmap-xhdpi'   = 96
    'mipmap-xxhdpi'  = 144
    'mipmap-xxxhdpi' = 192
}

foreach ($d in $densities.GetEnumerator()) {
    $dir = Join-Path $res $d.Key
    # Remove stale Image Asset Studio outputs (.webp) so png/webp never coexist
    # for the same resource name (would cause a duplicate-resource build error).
    foreach ($f in @('ic_launcher.webp', 'ic_launcher_foreground.webp')) {
        $p = Join-Path $dir $f
        if (Test-Path $p) { Remove-Item $p -Force; Write-Output "removed stale: $p" }
    }
    New-SquareIcon      $d.Value                      (Join-Path $dir 'ic_launcher.png')
    New-ForegroundIcon ([int]($d.Value * 108 / 48))   (Join-Path $dir 'ic_launcher_foreground.png')
}

$src.Dispose()
if ($script:srcRound) { $script:srcRound.Dispose() }
Write-Output "DONE"