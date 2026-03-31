"""
Scan Screen for Pocket AI
Receipt scanning with advanced OCR capabilities
"""

from kivy.lang import Builder
from kivy.properties import StringProperty, NumericProperty, BooleanProperty, ObjectProperty
from kivy.clock import Clock
from kivy.metrics import dp
from kivy.utils import get_color_from_hex, platform
from kivy.animation import Animation

from kivymd.uix.screen import MDScreen
from kivymd.uix.card import MDCard
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.button import MDIconButton, MDButton, MDButtonText
from kivymd.uix.progressindicator import MDCircularProgressIndicator

import os
import threading
from datetime import datetime

Builder.load_string('''
#:import get_color_from_hex kivy.utils.get_color_from_hex

<ScanScreen>:
    name: 'scan'
    md_bg_color: app.theme_cls.backgroundColor
    
    MDBoxLayout:
        orientation: 'vertical'
        
        # Header
        MDBoxLayout:
            adaptive_height: True
            padding: [dp(16), dp(16), dp(16), dp(8)]
            
            MDLabel:
                text: "Scan Receipt"
                font_style: "Headline"
                role: "medium"
                theme_text_color: "Primary"
                adaptive_height: True
                bold: True
        
        # Main Content
        MDBoxLayout:
            orientation: 'vertical'
            padding: dp(16)
            spacing: dp(24)
            
            # Scan Options (when not scanning)
            MDBoxLayout:
                id: scan_options
                orientation: 'vertical'
                spacing: dp(24)
                opacity: 1
                
                # Camera Option
                MDCard:
                    style: "elevated"
                    radius: [dp(24), dp(24), dp(24), dp(24)]
                    padding: dp(24)
                    md_bg_color: app.theme_cls.surfaceContainerColor
                    orientation: 'vertical'
                    spacing: dp(16)
                    on_release: root.open_camera()
                    ripple_behavior: True
                    
                    MDBoxLayout:
                        adaptive_height: True
                        pos_hint: {"center_x": 0.5}
                        size_hint_x: None
                        width: dp(80)
                        
                        MDCard:
                            style: "filled"
                            size_hint: None, None
                            size: dp(80), dp(80)
                            radius: [dp(20), dp(20), dp(20), dp(20)]
                            md_bg_color: [0.0, 0.737, 0.831, 0.15]  # #00BCD4 with 0.15 alpha
                            padding: dp(16)
                            
                            MDIcon:
                                icon: "camera"
                                halign: "center"
                                valign: "center"
                                theme_icon_color: "Custom"
                                icon_color: [0.0, 0.737, 0.831, 1.0]  # #00BCD4
                                font_size: dp(40)
                    
                    MDLabel:
                        text: "Take Photo"
                        font_style: "Title"
                        role: "large"
                        theme_text_color: "Primary"
                        adaptive_height: True
                        halign: "center"
                        bold: True
                        
                    MDLabel:
                        text: "Use your camera to capture a receipt"
                        font_style: "Body"
                        role: "medium"
                        theme_text_color: "Secondary"
                        adaptive_height: True
                        halign: "center"
                
                # Gallery Option
                MDCard:
                    style: "elevated"
                    radius: [dp(24), dp(24), dp(24), dp(24)]
                    padding: dp(24)
                    md_bg_color: app.theme_cls.surfaceContainerColor
                    orientation: 'vertical'
                    spacing: dp(16)
                    on_release: root.open_gallery()
                    ripple_behavior: True
                    
                    MDBoxLayout:
                        adaptive_height: True
                        pos_hint: {"center_x": 0.5}
                        size_hint_x: None
                        width: dp(80)
                        
                        MDCard:
                            style: "filled"
                            size_hint: None, None
                            size: dp(80), dp(80)
                            radius: [dp(20), dp(20), dp(20), dp(20)]
                            md_bg_color: [0.914, 0.118, 0.388, 0.15]  # #E91E63 with 0.15 alpha
                            padding: dp(16)
                            
                            MDIcon:
                                icon: "image"
                                halign: "center"
                                valign: "center"
                                theme_icon_color: "Custom"
                                icon_color: [0.914, 0.118, 0.388, 1.0]  # #E91E63
                                font_size: dp(40)
                    
                    MDLabel:
                        text: "Choose from Gallery"
                        font_style: "Title"
                        role: "large"
                        theme_text_color: "Primary"
                        adaptive_height: True
                        halign: "center"
                        bold: True
                        
                    MDLabel:
                        text: "Select an existing receipt image"
                        font_style: "Body"
                        role: "medium"
                        theme_text_color: "Secondary"
                        adaptive_height: True
                        halign: "center"
                
                # Manual Entry Option
                MDButton:
                    style: "outlined"
                    pos_hint: {"center_x": 0.5}
                    on_release: app.go_to_add_expense()
                    
                    MDButtonText:
                        text: "Or Enter Manually"
            
            # Processing View (when scanning)
            MDBoxLayout:
                id: processing_view
                orientation: 'vertical'
                spacing: dp(24)
                opacity: 0
                disabled: True
                
                Widget:
                    size_hint_y: 0.1
                
                # Progress Animation
                MDCard:
                    style: "elevated"
                    radius: [dp(24), dp(24), dp(24), dp(24)]
                    padding: dp(32)
                    md_bg_color: app.theme_cls.surfaceContainerColor
                    orientation: 'vertical'
                    adaptive_height: True
                    spacing: dp(24)
                    pos_hint: {"center_x": 0.5}
                    size_hint_x: 0.9
                    
                    MDBoxLayout:
                        adaptive_height: True
                        pos_hint: {"center_x": 0.5}
                        size_hint_x: None
                        width: dp(80)
                        
                        MDCircularProgressIndicator:
                            id: progress_indicator
                            size_hint: None, None
                            size: dp(80), dp(80)
                            pos_hint: {"center_x": 0.5}
                    
                    MDLabel:
                        id: status_label
                        text: "Scanning receipt..."
                        font_style: "Title"
                        role: "large"
                        theme_text_color: "Primary"
                        adaptive_height: True
                        halign: "center"
                        bold: True
                    
                    MDLabel:
                        id: substatus_label
                        text: "Analyzing image..."
                        font_style: "Body"
                        role: "medium"
                        theme_text_color: "Secondary"
                        adaptive_height: True
                        halign: "center"
                    
                    MDLabel:
                        id: time_label
                        text: "Estimated time: ~30 seconds"
                        font_style: "Label"
                        role: "large"
                        theme_text_color: "Custom"
                        text_color: [0.0, 0.737, 0.831, 0.8]  # #00BCD4 with 0.8 alpha
                        adaptive_height: True
                        halign: "center"
                
                Widget:
                    size_hint_y: 0.1
                
                MDButton:
                    style: "outlined"
                    pos_hint: {"center_x": 0.5}
                    on_release: root.cancel_scan()
                    
                    MDButtonText:
                        text: "Cancel"
''')


class ScanScreen(MDScreen):
    """Screen for scanning receipts with OCR"""
    
    is_processing = BooleanProperty(False)
    current_image_path = StringProperty('')
    
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.ocr_thread = None
        self.cancel_requested = False
    
    def open_camera(self):
        """Open camera to capture receipt"""
        if platform == 'android':
            self._open_android_camera()
        else:
            # For desktop testing, use file chooser
            self._open_file_chooser()
    
    def open_gallery(self):
        """Open gallery to select receipt image"""
        if platform == 'android':
            self._open_android_gallery()
        else:
            # For desktop testing, use file chooser
            self._open_file_chooser()
    
    def _open_android_camera(self):
        """Open Android camera"""
        try:
            from android.permissions import request_permissions, Permission
            from plyer import camera
            
            def on_permissions(permissions, grants):
                if all(grants):
                    # Create path for captured image
                    from android.storage import app_storage_path
                    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
                    image_path = os.path.join(app_storage_path(), f'receipt_{timestamp}.jpg')
                    
                    camera.take_picture(
                        filename=image_path,
                        on_complete=self._on_image_captured
                    )
            
            request_permissions([Permission.CAMERA, Permission.WRITE_EXTERNAL_STORAGE], on_permissions)
            
        except Exception as e:
            from kivymd.app import MDApp
            app = MDApp.get_running_app()
            app.show_snackbar(f"Camera error: {str(e)}")
    
    def _open_android_gallery(self):
        """Open Android gallery"""
        try:
            from android.permissions import request_permissions, Permission
            from plyer import filechooser
            
            def on_permissions(permissions, grants):
                if all(grants):
                    filechooser.open_file(
                        on_selection=self._on_file_selected,
                        filters=['*.jpg', '*.jpeg', '*.png']
                    )
            
            request_permissions([Permission.READ_EXTERNAL_STORAGE], on_permissions)
            
        except Exception as e:
            from kivymd.app import MDApp
            app = MDApp.get_running_app()
            app.show_snackbar(f"Gallery error: {str(e)}")
    
    def _open_file_chooser(self):
        """Open file chooser for desktop testing"""
        try:
            from plyer import filechooser
            filechooser.open_file(
                on_selection=self._on_file_selected,
                filters=['*.jpg', '*.jpeg', '*.png', '*.bmp']
            )
        except Exception as e:
            from kivymd.app import MDApp
            app = MDApp.get_running_app()
            app.show_snackbar(f"File chooser error: {str(e)}")
    
    def _on_image_captured(self, filepath):
        """Handle captured image from camera"""
        if filepath and os.path.exists(filepath):
            self.current_image_path = filepath
            self.start_ocr_processing()
    
    def _on_file_selected(self, selection):
        """Handle selected file from gallery/file chooser"""
        if selection and len(selection) > 0:
            filepath = selection[0]
            if os.path.exists(filepath):
                # Copy to app storage
                self.current_image_path = self._copy_to_app_storage(filepath)
                self.start_ocr_processing()
    
    def _copy_to_app_storage(self, source_path):
        """Copy image to app storage"""
        import shutil
        
        if platform == 'android':
            from android.storage import app_storage_path
            dest_dir = app_storage_path()
        else:
            dest_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        
        # Create receipts directory
        receipts_dir = os.path.join(dest_dir, 'receipts')
        os.makedirs(receipts_dir, exist_ok=True)
        
        # Generate unique filename
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        ext = os.path.splitext(source_path)[1]
        dest_path = os.path.join(receipts_dir, f'receipt_{timestamp}{ext}')
        
        shutil.copy2(source_path, dest_path)
        return dest_path
    
    def start_ocr_processing(self):
        """Start OCR processing on the image"""
        self.is_processing = True
        self.cancel_requested = False
        
        # Show processing view
        self.ids.scan_options.opacity = 0
        self.ids.scan_options.disabled = True
        self.ids.processing_view.opacity = 1
        self.ids.processing_view.disabled = False
        
        # Start OCR in background thread
        self.ocr_thread = threading.Thread(target=self._run_ocr)
        self.ocr_thread.start()
        
        # Start progress animation
        self._animate_progress()
    
    def _animate_progress(self):
        """Animate progress updates"""
        if not self.is_processing:
            return
        
        steps = [
            ("Preprocessing image...", "Adjusting contrast and brightness"),
            ("Detecting text regions...", "Finding receipt boundaries"),
            ("Running OCR engine...", "Extracting text from image"),
            ("Parsing receipt data...", "Identifying store, date, and amounts"),
            ("Finalizing results...", "Preparing extracted data"),
        ]
        
        self._progress_step = 0
        
        def update_step(dt):
            if not self.is_processing or self.cancel_requested:
                return False
            
            if self._progress_step < len(steps):
                status, substatus = steps[self._progress_step]
                self.ids.status_label.text = status
                self.ids.substatus_label.text = substatus
                
                remaining = (len(steps) - self._progress_step) * 6
                self.ids.time_label.text = f"Estimated time: ~{remaining} seconds"
                
                self._progress_step += 1
                return True
            return False
        
        Clock.schedule_interval(update_step, 6)
    
    def _run_ocr(self):
        """Run OCR processing (in background thread)"""
        try:
            from utils.ocr_processor import OCRProcessor
            
            processor = OCRProcessor()
            result = processor.process_receipt(self.current_image_path)
            
            if self.cancel_requested:
                return
            
            # Schedule UI update on main thread
            Clock.schedule_once(lambda dt: self._on_ocr_complete(result), 0)
            
        except Exception as e:
            Clock.schedule_once(lambda dt: self._on_ocr_error(str(e)), 0)
    
    def _on_ocr_complete(self, result):
        """Handle OCR completion"""
        self.is_processing = False
        self._reset_ui()
        
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        
        if result and result.get('success'):
            # Prepare prefill data for add expense screen
            prefill_data = {
                'store_name': result.get('store_name', ''),
                'amount': result.get('total_amount', 0),
                'date': result.get('date', datetime.now().strftime('%Y-%m-%d')),
                'category': self._guess_category(result.get('store_name', '')),
                'receipt_image': self.current_image_path,
                'note': result.get('items_text', '')
            }
            
            app.show_snackbar("Receipt scanned successfully!")
            app.go_to_add_expense(prefill_data)
        else:
            error_msg = result.get('error', 'Could not extract data from receipt') if result else 'OCR processing failed'
            app.show_snackbar(error_msg)
    
    def _on_ocr_error(self, error):
        """Handle OCR error"""
        self.is_processing = False
        self._reset_ui()
        
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        app.show_snackbar(f"OCR Error: {error}")
    
    def _guess_category(self, store_name):
        """Guess category based on store name"""
        store_lower = store_name.lower()
        
        grocery_keywords = ['supermarket', 'grocery', 'market', 'food', 'lidl', 'aldi', 'rewe', 'edeka', 'spar', 'billa', 'hofer']
        electronics_keywords = ['electronics', 'media', 'saturn', 'mediamarkt', 'apple', 'tech']
        fashion_keywords = ['fashion', 'clothing', 'h&m', 'zara', 'primark', 'clothes']
        health_keywords = ['pharmacy', 'apotheke', 'dm', 'rossmann', 'beauty', 'health']
        restaurant_keywords = ['restaurant', 'cafe', 'coffee', 'mcdonald', 'burger', 'pizza', 'starbucks']
        
        for keyword in grocery_keywords:
            if keyword in store_lower:
                return 'groceries'
        
        for keyword in electronics_keywords:
            if keyword in store_lower:
                return 'electronics'
        
        for keyword in fashion_keywords:
            if keyword in store_lower:
                return 'fashion'
        
        for keyword in health_keywords:
            if keyword in store_lower:
                return 'health_beauty'
        
        for keyword in restaurant_keywords:
            if keyword in store_lower:
                return 'food_dining'
        
        return 'other'
    
    def _reset_ui(self):
        """Reset UI to initial state"""
        self.ids.scan_options.opacity = 1
        self.ids.scan_options.disabled = False
        self.ids.processing_view.opacity = 0
        self.ids.processing_view.disabled = True
    
    def cancel_scan(self):
        """Cancel ongoing scan"""
        self.cancel_requested = True
        self.is_processing = False
        self._reset_ui()
        
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        app.show_snackbar("Scan cancelled")
