"""
Store Logos Manager for Pocket AI
Handles store logo display with fallback to initials-based logos
"""

import os
from kivy.utils import platform
from kivy.graphics import Color, Rectangle, RoundedRectangle
from kivy.graphics.texture import Texture
from kivy.core.image import Image as CoreImage

# Known store logos mapping (store name patterns -> logo filename)
KNOWN_STORES = {
    # Supermarkets
    'lidl': 'lidl.png',
    'aldi': 'aldi.png',
    'rewe': 'rewe.png',
    'edeka': 'edeka.png',
    'spar': 'spar.png',
    'billa': 'billa.png',
    'hofer': 'hofer.png',
    'penny': 'penny.png',
    'netto': 'netto.png',
    'kaufland': 'kaufland.png',
    'real': 'real.png',
    'metro': 'metro.png',
    
    # Electronics
    'mediamarkt': 'mediamarkt.png',
    'media markt': 'mediamarkt.png',
    'saturn': 'saturn.png',
    'apple': 'apple.png',
    'apple store': 'apple.png',
    
    # Fashion
    'h&m': 'hm.png',
    'zara': 'zara.png',
    'primark': 'primark.png',
    'c&a': 'ca.png',
    'uniqlo': 'uniqlo.png',
    
    # Health & Beauty
    'dm': 'dm.png',
    'rossmann': 'rossmann.png',
    'müller': 'mueller.png',
    'douglas': 'douglas.png',
    
    # Food & Dining
    'mcdonald': 'mcdonalds.png',
    "mcdonald's": 'mcdonalds.png',
    'starbucks': 'starbucks.png',
    'subway': 'subway.png',
    'burger king': 'burgerking.png',
    'kfc': 'kfc.png',
    
    # Home
    'ikea': 'ikea.png',
    'obi': 'obi.png',
    'bauhaus': 'bauhaus.png',
    'hornbach': 'hornbach.png',
    
    # Gas Stations
    'shell': 'shell.png',
    'bp': 'bp.png',
    'aral': 'aral.png',
    'omv': 'omv.png',
}

# Color palette for initials-based logos
LOGO_COLORS = [
    '#E91E63',  # Pink
    '#9C27B0',  # Purple
    '#673AB7',  # Deep Purple
    '#3F51B5',  # Indigo
    '#2196F3',  # Blue
    '#03A9F4',  # Light Blue
    '#00BCD4',  # Cyan
    '#009688',  # Teal
    '#4CAF50',  # Green
    '#8BC34A',  # Light Green
    '#CDDC39',  # Lime
    '#FFC107',  # Amber
    '#FF9800',  # Orange
    '#FF5722',  # Deep Orange
    '#795548',  # Brown
]


def get_logo_path(store_name):
    """
    Get the path to a store's logo image.
    Returns None if no logo is found.
    """
    if not store_name:
        return None
    
    store_lower = store_name.lower().strip()
    
    # Check for known stores
    for pattern, logo_file in KNOWN_STORES.items():
        if pattern in store_lower:
            logo_path = get_asset_path(f'logos/{logo_file}')
            if logo_path and os.path.exists(logo_path):
                return logo_path
    
    return None


def get_asset_path(relative_path):
    """Get the full path to an asset file."""
    if platform == 'android':
        from android.storage import app_storage_path
        base_path = app_storage_path()
    else:
        base_path = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    
    return os.path.join(base_path, 'assets', relative_path)


def get_initials(store_name, max_chars=2):
    """
    Get initials from a store name.
    
    Examples:
        "Lidl" -> "L"
        "Media Markt" -> "MM"
        "H&M" -> "H&"
    """
    if not store_name:
        return "?"
    
    # Clean the name
    name = store_name.strip()
    
    # Handle special characters
    if '&' in name:
        parts = name.split('&')
        if len(parts) >= 2:
            return (parts[0].strip()[0] + '&').upper()[:max_chars]
    
    # Split into words
    words = name.split()
    
    if len(words) == 1:
        # Single word - take first 1-2 characters
        return name[:max_chars].upper()
    else:
        # Multiple words - take first letter of each
        initials = ''.join(word[0] for word in words if word)
        return initials[:max_chars].upper()


def get_color_for_store(store_name):
    """
    Get a consistent color for a store based on its name.
    Uses a hash to ensure the same store always gets the same color.
    """
    if not store_name:
        return LOGO_COLORS[0]
    
    # Use hash to get consistent color
    hash_value = sum(ord(c) for c in store_name.lower())
    color_index = hash_value % len(LOGO_COLORS)
    
    return LOGO_COLORS[color_index]


def hex_to_rgba(hex_color, alpha=1.0):
    """Convert hex color to RGBA tuple."""
    hex_color = hex_color.lstrip('#')
    r = int(hex_color[0:2], 16) / 255.0
    g = int(hex_color[2:4], 16) / 255.0
    b = int(hex_color[4:6], 16) / 255.0
    return (r, g, b, alpha)


class StoreLogoWidget:
    """
    Helper class for creating store logo widgets.
    Can be used to generate logo textures for display.
    """
    
    @staticmethod
    def get_logo_texture(store_name, size=(64, 64)):
        """
        Get a texture for a store logo.
        Returns the actual logo if available, otherwise generates an initials-based logo.
        """
        logo_path = get_logo_path(store_name)
        
        if logo_path:
            try:
                return CoreImage(logo_path).texture
            except:
                pass
        
        # Generate initials-based logo
        return StoreLogoWidget.generate_initials_texture(store_name, size)
    
    @staticmethod
    def generate_initials_texture(store_name, size=(64, 64)):
        """
        Generate a texture with store initials.
        This creates a colored circle with the store's initials.
        """
        # This is a placeholder - in a full implementation,
        # we'd use Kivy's graphics to render the initials
        # For now, we return None and let the UI handle it
        return None


def create_logo_directory():
    """Create the logos directory if it doesn't exist."""
    logos_dir = get_asset_path('logos')
    os.makedirs(logos_dir, exist_ok=True)
    return logos_dir
