"""
Add Expense Screen for Pocket AI
Form for manually adding new expenses
"""

from kivy.lang import Builder
from kivy.properties import StringProperty, NumericProperty, ListProperty, ObjectProperty, BooleanProperty
from kivy.clock import Clock
from kivy.metrics import dp
from kivy.utils import get_color_from_hex

from kivymd.uix.screen import MDScreen
from kivymd.uix.card import MDCard
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.button import MDIconButton, MDButton, MDButtonText
from kivymd.uix.textfield import MDTextField
from kivymd.uix.menu import MDDropdownMenu
from kivymd.uix.pickers import MDDockedDatePicker

from datetime import datetime

Builder.load_string('''
<CategoryChip>:
    style: "elevated" if not root.is_selected else "filled"
    radius: [dp(12), dp(12), dp(12), dp(12)]
    padding: [dp(12), dp(8)]
    md_bg_color: root.bg_color
    size_hint: None, None
    height: dp(40)
    width: self.minimum_width
    on_release: root.on_select()
    ripple_behavior: True
    
    MDBoxLayout:
        adaptive_width: True
        spacing: dp(6)
        
        MDIcon:
            icon: root.chip_icon
            theme_icon_color: "Custom"
            icon_color: root.icon_color
            font_size: dp(18)
            
        MDLabel:
            text: root.chip_text
            font_style: "Label"
            role: "large"
            theme_text_color: "Custom"
            text_color: root.text_color
            adaptive_width: True

<AddExpenseScreen>:
    name: 'add_expense'
    md_bg_color: app.theme_cls.backgroundColor
    
    MDBoxLayout:
        orientation: 'vertical'
        
        # Header
        MDBoxLayout:
            adaptive_height: True
            padding: [dp(4), dp(8), dp(16), dp(8)]
            spacing: dp(8)
            
            MDIconButton:
                icon: "arrow-left"
                style: "standard"
                on_release: app.go_back()
                
            MDLabel:
                text: "Add Expense"
                font_style: "Headline"
                role: "small"
                theme_text_color: "Primary"
                adaptive_height: True
                bold: True
                pos_hint: {"center_y": 0.5}
        
        MDScrollView:
            do_scroll_x: False
            
            MDBoxLayout:
                orientation: 'vertical'
                padding: dp(16)
                spacing: dp(20)
                adaptive_height: True
                
                # Amount Input (Large, prominent)
                MDCard:
                    style: "elevated"
                    radius: [dp(20), dp(20), dp(20), dp(20)]
                    padding: dp(24)
                    md_bg_color: app.theme_cls.surfaceContainerColor
                    orientation: 'vertical'
                    adaptive_height: True
                    spacing: dp(8)
                    
                    MDLabel:
                        text: "Amount"
                        font_style: "Label"
                        role: "large"
                        theme_text_color: "Secondary"
                        adaptive_height: True
                        halign: "center"
                    
                    MDBoxLayout:
                        adaptive_height: True
                        spacing: dp(4)
                        pos_hint: {"center_x": 0.5}
                        size_hint_x: None
                        width: dp(200)
                        
                        MDLabel:
                            text: "€"
                            font_style: "Display"
                            role: "small"
                            theme_text_color: "Custom"
                            text_color: 0, 0.74, 0.83, 1
                            adaptive_height: True
                            size_hint_x: None
                            width: dp(40)
                            halign: "right"
                            bold: True
                            
                        MDTextField:
                            id: amount_field
                            mode: "outlined"
                            hint_text: "0.00"
                            input_filter: "float"
                            font_size: sp(32)
                            halign: "left"
                
                # Store Name
                MDTextField:
                    id: store_field
                    mode: "outlined"
                    hint_text: "Store Name"
                    
                    MDTextFieldLeadingIcon:
                        icon: "store"
                        
                    MDTextFieldHintText:
                        text: "Store Name"
                
                # Category Selection
                MDBoxLayout:
                    orientation: 'vertical'
                    adaptive_height: True
                    spacing: dp(12)
                    
                    MDLabel:
                        text: "Category"
                        font_style: "Label"
                        role: "large"
                        theme_text_color: "Secondary"
                        adaptive_height: True
                    
                    MDBoxLayout:
                        id: category_row_1
                        adaptive_height: True
                        spacing: dp(8)
                        
                    MDBoxLayout:
                        id: category_row_2
                        adaptive_height: True
                        spacing: dp(8)
                        
                    MDBoxLayout:
                        id: category_row_3
                        adaptive_height: True
                        spacing: dp(8)
                
                # Date Selection
                MDBoxLayout:
                    adaptive_height: True
                    spacing: dp(12)
                    
                    MDTextField:
                        id: date_field
                        mode: "outlined"
                        hint_text: "Date"
                        text: root.selected_date
                        readonly: True
                        on_focus: root.show_date_picker() if self.focus else None
                        
                        MDTextFieldLeadingIcon:
                            icon: "calendar"
                            
                        MDTextFieldHintText:
                            text: "Date"
                    
                    MDIconButton:
                        icon: "calendar-today"
                        style: "tonal"
                        on_release: root.set_today()
                        pos_hint: {"center_y": 0.5}
                
                # Note (Optional)
                MDTextField:
                    id: note_field
                    mode: "outlined"
                    hint_text: "Note (optional)"
                    multiline: True
                    max_height: dp(100)
                    
                    MDTextFieldLeadingIcon:
                        icon: "note-text"
                        
                    MDTextFieldHintText:
                        text: "Note (optional)"
                
                # Receipt Image Preview (if from scan)
                MDBoxLayout:
                    id: receipt_preview_container
                    orientation: 'vertical'
                    adaptive_height: True
                    opacity: 0
                    size_hint_y: None
                    height: 0
                
                # Save Button
                MDButton:
                    style: "filled"
                    size_hint_x: 1
                    on_release: root.save_expense()
                    
                    MDButtonText:
                        text: "Save Expense"
                        
                Widget:
                    size_hint_y: None
                    height: dp(32)
''')


class CategoryChip(MDCard):
    """Chip for category selection"""
    chip_text = StringProperty("")
    chip_icon = StringProperty("receipt")
    chip_color = StringProperty("#00BCD4")
    is_selected = BooleanProperty(False)
    category_id = StringProperty("")
    icon_color = ListProperty([0, 0.74, 0.83, 1])
    bg_color = ListProperty([0, 0.74, 0.83, 0.05])
    text_color = ListProperty([1, 1, 1, 1])
    
    def on_select(self):
        """Handle chip selection"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        screen = app.root.ids.screen_manager.get_screen('add_expense')
        screen.select_category(self.category_id)


class AddExpenseScreen(MDScreen):
    """Screen for adding new expenses"""
    
    selected_category = StringProperty('other')
    selected_date = StringProperty('')
    receipt_image_path = StringProperty('')
    
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.category_chips = {}
        self.date_picker = None
        Clock.schedule_once(self.setup_categories, 0.5)
    
    def setup_categories(self, dt=None):
        """Setup category chips"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        db = app.db
        
        # Clear existing chips
        self.ids.category_row_1.clear_widgets()
        self.ids.category_row_2.clear_widgets()
        self.ids.category_row_3.clear_widgets()
        self.category_chips = {}
        
        # Create chips for each category
        categories = db.CATEGORIES
        rows = [self.ids.category_row_1, self.ids.category_row_2, self.ids.category_row_3]
        
        for i, (cat_id, cat_name, cat_icon) in enumerate(categories):
            hex_color = db.CATEGORY_COLORS.get(cat_id, '#607D8B')
            rgba = list(get_color_from_hex(hex_color))
            
            is_selected = (cat_id == self.selected_category)
            
            chip = CategoryChip(
                chip_text=cat_name,
                chip_icon=cat_icon,
                chip_color=hex_color,
                category_id=cat_id,
                is_selected=is_selected,
                icon_color=rgba,
                bg_color=rgba[:3] + [0.2 if is_selected else 0.05],
                text_color=rgba if is_selected else [1, 1, 1, 0.87]
            )
            
            self.category_chips[cat_id] = chip
            
            # Distribute chips across rows
            row_index = i // 3
            if row_index < len(rows):
                rows[row_index].add_widget(chip)
    
    def select_category(self, category_id):
        """Select a category"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        db = app.db
        
        self.selected_category = category_id
        
        # Update chip states
        for cat_id, chip in self.category_chips.items():
            is_selected = (cat_id == category_id)
            chip.is_selected = is_selected
            
            hex_color = db.CATEGORY_COLORS.get(cat_id, '#607D8B')
            rgba = list(get_color_from_hex(hex_color))
            
            chip.bg_color = rgba[:3] + [0.2 if is_selected else 0.05]
            chip.text_color = rgba if is_selected else [1, 1, 1, 0.87]
    
    def reset_form(self, prefill_data=None):
        """Reset the form, optionally with prefilled data"""
        # Set default date to today
        self.selected_date = datetime.now().strftime('%Y-%m-%d')
        self.selected_category = 'other'
        self.receipt_image_path = ''
        
        # Clear fields
        Clock.schedule_once(lambda dt: self._clear_fields(prefill_data), 0.1)
    
    def _clear_fields(self, prefill_data=None):
        """Clear form fields"""
        self.ids.amount_field.text = ''
        self.ids.store_field.text = ''
        self.ids.note_field.text = ''
        
        # Hide receipt preview
        self.ids.receipt_preview_container.opacity = 0
        self.ids.receipt_preview_container.height = 0
        
        # Prefill data if provided (from OCR scan)
        if prefill_data:
            if 'amount' in prefill_data:
                self.ids.amount_field.text = str(prefill_data['amount'])
            if 'store_name' in prefill_data:
                self.ids.store_field.text = prefill_data['store_name']
            if 'date' in prefill_data:
                self.selected_date = prefill_data['date']
            if 'category' in prefill_data:
                self.select_category(prefill_data['category'])
            if 'note' in prefill_data:
                self.ids.note_field.text = prefill_data['note']
            if 'receipt_image' in prefill_data:
                self.receipt_image_path = prefill_data['receipt_image']
        
        # Update category chips
        self.setup_categories()
    
    def show_date_picker(self):
        """Show date picker dialog"""
        if not self.date_picker:
            self.date_picker = MDDockedDatePicker()
            self.date_picker.bind(on_ok=self.on_date_selected)
            self.date_picker.bind(on_cancel=self.on_date_cancel)
        
        # Set current date
        try:
            current = datetime.strptime(self.selected_date, '%Y-%m-%d')
            self.date_picker.set_date(current.year, current.month, current.day)
        except:
            pass
        
        self.date_picker.open()
    
    def on_date_selected(self, instance):
        """Handle date selection"""
        selected = instance.get_date()
        if selected:
            self.selected_date = selected[0].strftime('%Y-%m-%d')
        instance.dismiss()
    
    def on_date_cancel(self, instance):
        """Handle date picker cancel"""
        instance.dismiss()
    
    def set_today(self):
        """Set date to today"""
        self.selected_date = datetime.now().strftime('%Y-%m-%d')
    
    def save_expense(self):
        """Save the expense to database"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        db = app.db
        
        # Validate inputs
        amount_text = self.ids.amount_field.text.strip()
        store_name = self.ids.store_field.text.strip()
        
        if not amount_text:
            app.show_snackbar("Please enter an amount")
            return
        
        try:
            amount = float(amount_text)
        except ValueError:
            app.show_snackbar("Invalid amount")
            return
        
        if amount <= 0:
            app.show_snackbar("Amount must be greater than 0")
            return
        
        if not store_name:
            app.show_snackbar("Please enter a store name")
            return
        
        # Get optional note
        note = self.ids.note_field.text.strip() or None
        
        # Save to database
        expense_id = db.add_expense(
            store_name=store_name,
            category=self.selected_category,
            amount=amount,
            date=self.selected_date,
            note=note,
            receipt_image=self.receipt_image_path or None
        )
        
        # Show success message
        app.show_snackbar("Expense saved successfully!")
        
        # Navigate back to receipts
        app.refresh_receipts()
        app.go_back()
