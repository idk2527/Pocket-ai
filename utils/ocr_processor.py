"""
OCR Processor for Pocket AI
Advanced receipt scanning with image preprocessing and text extraction
"""

import os
import re
from datetime import datetime
from kivy.utils import platform

# Try to import image processing libraries
try:
    from PIL import Image, ImageEnhance, ImageFilter
    PIL_AVAILABLE = True
except ImportError:
    PIL_AVAILABLE = False

# Try to import OCR library
try:
    import pytesseract
    TESSERACT_AVAILABLE = True
except ImportError:
    TESSERACT_AVAILABLE = False


class OCRProcessor:
    """
    Advanced OCR processor for receipt scanning.
    Handles image preprocessing, text extraction, and data parsing.
    """
    
    # Common store name patterns
    STORE_PATTERNS = [
        r'^([A-Z][A-Za-z\s&\']+)(?:\s*(?:GmbH|AG|Inc|Ltd|LLC))?',
        r'(?:Welcome to|Thank you for shopping at)\s+([A-Za-z\s&\']+)',
    ]
    
    # Date patterns (various formats)
    DATE_PATTERNS = [
        r'(\d{1,2}[./]\d{1,2}[./]\d{2,4})',  # DD/MM/YYYY or DD.MM.YYYY
        r'(\d{4}[.-]\d{2}[.-]\d{2})',  # YYYY-MM-DD
        r'(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{2,4})',  # DD Mon YYYY
    ]
    
    # Amount patterns
    AMOUNT_PATTERNS = [
        r'(?:Total|TOTAL|Summe|SUMME|Gesamt|GESAMT|Amount|AMOUNT|Due|DUE)[:\s]*[€$£]?\s*(\d+[.,]\d{2})',
        r'(?:Total|TOTAL|Summe|SUMME|Gesamt|GESAMT)[:\s]*(\d+[.,]\d{2})\s*[€$£]?',
        r'[€$£]\s*(\d+[.,]\d{2})\s*$',
        r'(\d+[.,]\d{2})\s*[€$£]?\s*(?:EUR|USD|GBP)?$',
    ]
    
    # Currency patterns
    CURRENCY_PATTERNS = [
        (r'[€]|EUR', 'EUR'),
        (r'[$]|USD', 'USD'),
        (r'[£]|GBP', 'GBP'),
    ]
    
    def __init__(self):
        self.debug_mode = False
    
    def process_receipt(self, image_path):
        """
        Process a receipt image and extract data.
        
        Args:
            image_path: Path to the receipt image
            
        Returns:
            dict with extracted data or error information
        """
        if not os.path.exists(image_path):
            return {'success': False, 'error': 'Image file not found'}
        
        # Check if required libraries are available
        if not PIL_AVAILABLE:
            return self._fallback_processing(image_path)
        
        try:
            # Step 1: Load and preprocess image
            processed_image = self._preprocess_image(image_path)
            
            # Step 2: Extract text using OCR
            text = self._extract_text(processed_image)
            
            if not text or len(text.strip()) < 10:
                return {'success': False, 'error': 'Could not extract text from image'}
            
            # Step 3: Parse extracted text
            result = self._parse_receipt_text(text)
            result['success'] = True
            result['raw_text'] = text
            
            return result
            
        except Exception as e:
            return {'success': False, 'error': str(e)}
    
    def _preprocess_image(self, image_path):
        """
        Preprocess image for better OCR accuracy.
        Applies contrast enhancement, sharpening, and binarization.
        """
        if not PIL_AVAILABLE:
            return image_path
        
        # Load image
        image = Image.open(image_path)
        
        # Convert to RGB if necessary
        if image.mode != 'RGB':
            image = image.convert('RGB')
        
        # Resize if too large (for faster processing)
        max_dimension = 2000
        if max(image.size) > max_dimension:
            ratio = max_dimension / max(image.size)
            new_size = (int(image.size[0] * ratio), int(image.size[1] * ratio))
            image = image.resize(new_size, Image.Resampling.LANCZOS)
        
        # Convert to grayscale
        gray = image.convert('L')
        
        # Enhance contrast
        enhancer = ImageEnhance.Contrast(gray)
        gray = enhancer.enhance(2.0)
        
        # Enhance sharpness
        enhancer = ImageEnhance.Sharpness(gray)
        gray = enhancer.enhance(2.0)
        
        # Apply slight blur to reduce noise
        gray = gray.filter(ImageFilter.MedianFilter(size=3))
        
        # Binarization (convert to black and white)
        threshold = 140
        gray = gray.point(lambda x: 255 if x > threshold else 0, mode='1')
        
        return gray
    
    def _extract_text(self, image):
        """
        Extract text from preprocessed image using OCR.
        """
        if TESSERACT_AVAILABLE:
            # Configure Tesseract for receipt scanning
            custom_config = r'--oem 3 --psm 6 -l eng+deu'
            
            try:
                text = pytesseract.image_to_string(image, config=custom_config)
                return text
            except Exception as e:
                # Fallback to basic config
                try:
                    text = pytesseract.image_to_string(image)
                    return text
                except:
                    pass
        
        # If Tesseract is not available, return empty
        # In production, we'd use an alternative OCR service
        return ""
    
    def _parse_receipt_text(self, text):
        """
        Parse extracted text to find store name, date, total, and items.
        """
        lines = text.split('\n')
        lines = [line.strip() for line in lines if line.strip()]
        
        result = {
            'store_name': '',
            'date': datetime.now().strftime('%Y-%m-%d'),
            'total_amount': 0.0,
            'currency': 'EUR',
            'items': [],
            'items_text': ''
        }
        
        # Extract store name (usually in first few lines)
        result['store_name'] = self._extract_store_name(lines[:5])
        
        # Extract date
        result['date'] = self._extract_date(text)
        
        # Extract total amount
        result['total_amount'], result['currency'] = self._extract_total(text, lines)
        
        # Extract items
        result['items'], result['items_text'] = self._extract_items(lines)
        
        return result
    
    def _extract_store_name(self, first_lines):
        """Extract store name from first few lines of receipt."""
        for line in first_lines:
            # Skip very short lines or lines with only numbers
            if len(line) < 3 or line.replace(' ', '').isdigit():
                continue
            
            # Skip lines that look like addresses or dates
            if any(x in line.lower() for x in ['str.', 'straße', 'street', 'avenue', 'tel', 'fax', '@']):
                continue
            
            # Check against patterns
            for pattern in self.STORE_PATTERNS:
                match = re.search(pattern, line, re.IGNORECASE)
                if match:
                    return match.group(1).strip()
            
            # If line looks like a store name (mostly letters, reasonable length)
            if len(line) >= 3 and len(line) <= 50:
                # Remove common suffixes
                name = re.sub(r'\s*(GmbH|AG|Inc|Ltd|LLC|Co\.?)\s*$', '', line, flags=re.IGNORECASE)
                if name and not name.isdigit():
                    return name.strip()
        
        return "Unknown Store"
    
    def _extract_date(self, text):
        """Extract date from receipt text."""
        for pattern in self.DATE_PATTERNS:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                date_str = match.group(1)
                
                # Try to parse the date
                date_formats = [
                    '%d/%m/%Y', '%d.%m.%Y', '%d-%m-%Y',
                    '%d/%m/%y', '%d.%m.%y', '%d-%m-%y',
                    '%Y-%m-%d', '%Y.%m.%d', '%Y/%m/%d',
                    '%d %b %Y', '%d %B %Y',
                ]
                
                for fmt in date_formats:
                    try:
                        parsed = datetime.strptime(date_str, fmt)
                        return parsed.strftime('%Y-%m-%d')
                    except ValueError:
                        continue
        
        # Default to today
        return datetime.now().strftime('%Y-%m-%d')
    
    def _extract_total(self, text, lines):
        """Extract total amount and currency from receipt."""
        currency = 'EUR'  # Default
        
        # Detect currency
        for pattern, curr in self.CURRENCY_PATTERNS:
            if re.search(pattern, text):
                currency = curr
                break
        
        # Find total amount
        amounts = []
        
        for pattern in self.AMOUNT_PATTERNS:
            matches = re.findall(pattern, text, re.MULTILINE | re.IGNORECASE)
            for match in matches:
                try:
                    # Normalize decimal separator
                    amount_str = match.replace(',', '.')
                    amount = float(amount_str)
                    if amount > 0:
                        amounts.append(amount)
                except ValueError:
                    continue
        
        # Also look for the largest amount (often the total)
        all_amounts = re.findall(r'(\d+[.,]\d{2})', text)
        for match in all_amounts:
            try:
                amount_str = match.replace(',', '.')
                amount = float(amount_str)
                if amount > 0:
                    amounts.append(amount)
            except ValueError:
                continue
        
        if amounts:
            # The total is usually the largest amount, or appears near "Total"
            # For now, take the maximum as a heuristic
            return max(amounts), currency
        
        return 0.0, currency
    
    def _extract_items(self, lines):
        """Extract individual items from receipt."""
        items = []
        items_text_parts = []
        
        # Pattern for item lines: description followed by price
        item_pattern = r'^(.+?)\s+(\d+[.,]\d{2})\s*[€$£]?\s*$'
        
        for line in lines:
            # Skip header/footer lines
            if any(x in line.lower() for x in ['total', 'summe', 'gesamt', 'subtotal', 'tax', 'mwst', 'change', 'cash', 'card']):
                continue
            
            match = re.match(item_pattern, line)
            if match:
                item_name = match.group(1).strip()
                try:
                    item_price = float(match.group(2).replace(',', '.'))
                    if item_name and item_price > 0:
                        items.append({
                            'name': item_name,
                            'price': item_price
                        })
                        items_text_parts.append(f"{item_name}: €{item_price:.2f}")
                except ValueError:
                    continue
        
        items_text = '\n'.join(items_text_parts) if items_text_parts else ''
        
        return items, items_text
    
    def _fallback_processing(self, image_path):
        """
        Fallback processing when PIL/Tesseract are not available.
        Returns a template for manual entry.
        """
        return {
            'success': True,
            'store_name': '',
            'date': datetime.now().strftime('%Y-%m-%d'),
            'total_amount': 0.0,
            'currency': 'EUR',
            'items': [],
            'items_text': '',
            'raw_text': '',
            'note': 'OCR libraries not available. Please enter details manually.'
        }


class ImagePreprocessor:
    """
    Additional image preprocessing utilities for receipt scanning.
    """
    
    @staticmethod
    def auto_rotate(image):
        """Auto-rotate image based on EXIF data."""
        if not PIL_AVAILABLE:
            return image
        
        try:
            from PIL import ExifTags
            
            for orientation in ExifTags.TAGS.keys():
                if ExifTags.TAGS[orientation] == 'Orientation':
                    break
            
            exif = image._getexif()
            if exif is not None:
                orientation_value = exif.get(orientation)
                
                if orientation_value == 3:
                    image = image.rotate(180, expand=True)
                elif orientation_value == 6:
                    image = image.rotate(270, expand=True)
                elif orientation_value == 8:
                    image = image.rotate(90, expand=True)
        except:
            pass
        
        return image
    
    @staticmethod
    def deskew(image):
        """Deskew (straighten) a tilted image."""
        if not PIL_AVAILABLE:
            return image
        
        # This is a simplified deskew - in production, we'd use
        # more sophisticated algorithms (Hough transform, etc.)
        return image
    
    @staticmethod
    def crop_receipt(image):
        """Auto-crop to receipt boundaries."""
        if not PIL_AVAILABLE:
            return image
        
        # This would use edge detection to find receipt boundaries
        # For now, return original image
        return image
