#!/usr/bin/env python3
"""
extract_video_frames.py
=======================
Extrae frames de un video de evidencia de test (MP4, MOV, AVI, etc.)
y los guarda como imágenes PNG en una carpeta de salida.

Uso:
    python3 extract_video_frames.py <ruta_del_video> [opciones]

Ejemplos:
    python3 extract_video_frames.py evidencia.mp4
    python3 extract_video_frames.py evidencia.mp4 --fps 0.5
    python3 extract_video_frames.py evidencia.mp4 --fps 2 --output /tmp/frames
    python3 extract_video_frames.py evidencia.mp4 --keyframes

Argumentos:
    video           Ruta al archivo de video (obligatorio)
    --fps           Frames por segundo a extraer (default: 1.0)
                    0.5 = 1 frame cada 2 segundos
                    2   = 2 frames por segundo
    --output / -o   Carpeta de salida (default: /tmp/video_frames_<nombre_video>)
    --keyframes     Solo extrae keyframes del video (ignora --fps)
    --max           Número máximo de frames a extraer (default: sin límite)
    --quality / -q  Calidad PNG (1-9, default: 3 — balance tamaño/calidad)

Requiere: ffmpeg (brew install ffmpeg)
"""

import argparse
import os
import subprocess
import sys
import shutil
from pathlib import Path

FFMPEG = '/opt/homebrew/bin/ffmpeg'
FFPROBE = '/opt/homebrew/bin/ffprobe'


def check_ffmpeg():
    """Verifica que ffmpeg esté disponible."""
    if not Path(FFMPEG).exists():
        # Fallback: buscar en PATH
        fallback = shutil.which('ffmpeg')
        if not fallback:
            print("❌ ffmpeg no encontrado. Instálalo con: brew install ffmpeg", file=sys.stderr)
            sys.exit(1)
        return fallback, shutil.which('ffprobe') or 'ffprobe'
    return FFMPEG, FFPROBE


def get_video_info(ffprobe: str, video_path: str) -> dict:
    """Obtiene duración y resolución del video."""
    cmd = [
        ffprobe, '-v', 'quiet', '-print_format', 'json',
        '-show_streams', '-select_streams', 'v:0', video_path
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        return {}

    import json
    try:
        data = json.loads(result.stdout)
        stream = data.get('streams', [{}])[0]
        duration = float(stream.get('duration', 0))
        width = stream.get('width', '?')
        height = stream.get('height', '?')
        nb_frames = stream.get('nb_frames', '?')
        r_frame_rate = stream.get('r_frame_rate', '?')
        return {
            'duration': duration,
            'width': width,
            'height': height,
            'nb_frames': nb_frames,
            'fps': r_frame_rate
        }
    except Exception:
        return {}


def extract_frames(
    video_path: str,
    output_dir: str,
    fps: float = 1.0,
    keyframes_only: bool = False,
    max_frames: int = None,
    quality: int = 3
) -> list[str]:
    """
    Extrae frames del video usando ffmpeg.
    Retorna lista de rutas de los frames extraídos.
    """
    ffmpeg, ffprobe = check_ffmpeg()
    video_path = os.path.abspath(video_path)

    if not os.path.isfile(video_path):
        print(f"❌ Archivo no encontrado: {video_path}", file=sys.stderr)
        sys.exit(1)

    # Crear carpeta de salida
    os.makedirs(output_dir, exist_ok=True)

    print(f"\n📹 Video: {video_path}")

    # Info del video
    info = get_video_info(ffprobe, video_path)
    if info:
        duration = info.get('duration', 0)
        print(f"   Duración:    {duration:.1f}s ({duration/60:.1f} min)")
        print(f"   Resolución:  {info['width']}x{info['height']}")
        print(f"   FPS nativo:  {info['fps']}")

    print(f"📁 Carpeta de salida: {output_dir}")

    # Construir filtro ffmpeg
    if keyframes_only:
        vf_filter = "select='eq(pict_type,I)'"
        print(f"   Modo: solo keyframes")
    else:
        vf_filter = f"fps={fps}"
        print(f"   Extrayendo: {fps} fps ({1/fps:.1f}s entre frames)")

    # Comando ffmpeg
    pattern = os.path.join(output_dir, 'frame_%04d.png')
    cmd = [
        ffmpeg, '-i', video_path,
        '-vf', vf_filter,
        '-compression_level', str(quality),
        '-vsync', 'vfr',
        pattern,
        '-y', '-hide_banner', '-loglevel', 'error'
    ]

    print("\n⏳ Extrayendo frames...")
    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.returncode != 0:
        print(f"❌ Error de ffmpeg:\n{result.stderr}", file=sys.stderr)
        sys.exit(1)

    # Listar frames generados
    frames = sorted([
        os.path.join(output_dir, f)
        for f in os.listdir(output_dir)
        if f.startswith('frame_') and f.endswith('.png')
    ])

    # Aplicar límite máximo si se especificó
    if max_frames and len(frames) > max_frames:
        # Seleccionar frames distribuidos uniformemente
        step = len(frames) / max_frames
        selected = [frames[int(i * step)] for i in range(max_frames)]
        # Eliminar frames no seleccionados
        for f in frames:
            if f not in selected:
                os.remove(f)
        frames = selected

    print(f"\n✅ {len(frames)} frames extraídos")

    return frames


def print_summary(frames: list[str], output_dir: str):
    """Imprime un resumen de los frames extraídos."""
    if not frames:
        print("⚠️  No se extrajeron frames.", file=sys.stderr)
        return

    total_size = sum(os.path.getsize(f) for f in frames) / (1024 * 1024)
    print(f"\n{'='*55}")
    print(f"  FRAMES EXTRAÍDOS: {len(frames)}")
    print(f"  Tamaño total:     {total_size:.1f} MB")
    print(f"  Carpeta:          {output_dir}")
    print(f"{'─'*55}")
    print("  Archivos (primeros y últimos 3):")

    show = frames if len(frames) <= 6 else frames[:3] + ['  ...'] + frames[-3:]
    for f in show:
        if f.startswith('  '):
            print(f)
        else:
            size_kb = os.path.getsize(f) / 1024
            print(f"    {os.path.basename(f)}  ({size_kb:.0f} KB)")

    print(f"{'='*55}")
    print("\n💡 Para analizar con el agente:")
    print(f"   Comparte los frames de: {output_dir}")
    print(f"   O dile al agente: 'analiza los frames en {output_dir}'")


def main():
    parser = argparse.ArgumentParser(
        description='Extrae frames de video de evidencia de test',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument('video', help='Ruta al archivo de video')
    parser.add_argument(
        '--fps', type=float, default=1.0,
        help='Frames por segundo a extraer (default: 1.0)'
    )
    parser.add_argument(
        '--output', '-o', type=str, default=None,
        help='Carpeta de salida (default: /tmp/video_frames_<nombre>)'
    )
    parser.add_argument(
        '--keyframes', action='store_true',
        help='Extraer solo keyframes del video'
    )
    parser.add_argument(
        '--max', type=int, default=None,
        help='Número máximo de frames (selección uniforme)'
    )
    parser.add_argument(
        '--quality', '-q', type=int, default=3, choices=range(1, 10),
        help='Calidad compresión PNG 1-9 (default: 3)'
    )

    args = parser.parse_args()

    # Carpeta de salida por defecto
    if not args.output:
        video_name = Path(args.video).stem
        args.output = f"/tmp/video_frames_{video_name}"

    frames = extract_frames(
        video_path=args.video,
        output_dir=args.output,
        fps=args.fps,
        keyframes_only=args.keyframes,
        max_frames=args.max,
        quality=args.quality
    )

    print_summary(frames, args.output)

    # Imprimir rutas absolutas para uso del agente
    print("\n📋 Rutas para el agente (copia y pega):")
    for f in frames:
        print(f"   {f}")


if __name__ == '__main__':
    main()
