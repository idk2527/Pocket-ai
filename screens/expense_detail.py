"""
Expense Detail Screen for Pocket AI
Shows detailed view of a single expense
"""

from kivy.lang import Builder
from kivy.properties import StringProperty, NumericProperty, BooleanProperty
from kivy.clock import Clock
from kivy.metrics import dp
from kivy.utils import get_color_from_hex

from kivymd.uix.screen import MDScreen
from kivymd.uix.card import MDCard
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.button import MDIconButton, MDButton, MDButtonText
from kivymd.uix.dialog import MDDialog, MDDialogHeadlineText, MDDialogSupportingText, MDDialogButtonContainer
from kivymd.uix.fitimage import FitImage

from datetime import datetime
import os

Builder.load_string('''
#:import get_color_from_hex kivy.utils.get_color_from_hex

<DetailRow@MDBoxLayout>:
    row_icon: ""
    row_label: ""
    row_value: ""
    adaptive_height: True
    spacing: dp(12)
    padding: [0, dp(8)]
    
    MDIcon:
        icon: root.row_icon
        theme_icon_color: "Custom"
        icon_color: [0.0, 0.737, 0.831, 0.7]  # #00BCD4 with 0.7 alpha
        pos_hint: {"center_y": 0.5}
        
    MDBoxLayout:
        orientation: 'vertical'
        adaptive_height: True
        
        MDLabel:
            text: root.row_label
            font_style: "Label"
            role: "medium"
            theme_text_color: "Secondary"
            adaptive_height: True
            
        MDLabel:
            text: root.row_value
            font_style: "Body"
            role: "large"
            theme_text_color: "Primary"
            adaptive_height: True

<ExpenseDetailScreen>:
    name: 'expense_detail'
    md_bg_color: app.theme_cls.backgroundColor
    
    MDBoxLayout:
        orientation: 'vertical'
        
        # Header
        MDBoxLayout:
            adaptive_height: True
            padding: [dp(4), dp(8), dp(8), dp(8)]
            spacing: dp(8)
            
            MDIconButton:
                icon: "arrow-left"
                style: "standard"
                on_release: app.go_back()
                
            MDLabel:
                text: "Expense Details"
                font_style: "Headline"
                role: "small"
                theme_text_color: "Primary"
                adaptive_height: True
                bold: True
                pos_hint: {"center_y": 0.5}
            
            MDIconButton:
                icon: "pencil"
                style: "standard"
                on_release: root.edit_expense()
                
            MDIconButton:
                icon: "delete"
                style: "standard"
                theme_icon_color: "Custom"
                icon_color: [0.914, 0.267, 0.212, 1.0]  # #F44336
                on_release: root.confirm_delete()
        
        MDScrollView:
            do_scroll_x: False
            
            MDBoxLayout:
                orientation: 'vertical'
                padding: dp(16)
                spacing: dp(16)
                adaptive_height: True
                
                # Main Amount Card
                MDCard:
                    style: "elevated"
                    radius: [dp(24), dp(24), dp(24), dp(24)]
                    padding: dp(24)
                    md_bg_color: app.theme_cls.surfaceContainerColor
                    orientation: 'vertical'
                    adaptive_height: True
                    spacing: dp(16)
                    
                    # Category Icon
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
                            md_bg_color: root.icon_bg_color_rgba  # Dynamic color
                            padding: dp(16)
                            pos_hint: {"center_x": 0.5}
                            
                            MDIcon:
                                icon: root.category_icon
                                halign: "center"
                                valign: "center"
                                theme_icon_color: "Custom"
                                icon_color: root.icon_color_rgba  # Dynamic color
                                font_size: dp(40)
                    
                    # Store Name
                    MDLabel:
                        text: root.store_name
                        font_style: "Headline"
                        role: "medium"
                        theme_text_color: "Primary"
                        adaptive_height: True
                        halign: "center"
                        bold: True
                    
                    # Amount
                    MDLabel:
                        text: root.amount_text
                        font_style: "Display"
                        role: "medium"
                        theme_text_color: "Custom"
                        text_color: [0.0, 0.737, 0.831, 1.0]  # #00BCD4
                        adaptive_height: True
                        halign: "center"
                        bold: True
                    
                    # Category Badge
                    MDBoxLayout:
                        adaptive_height: True
                        pos_hint: {"center_x": 0.5}
                        size_hint_x: None
                        width: self.minimum_width
                        
                        MDCard:
                            style: "filled"
                            radius: [dp(8), dp(8), dp(8), dp(8)]
                            padding: [dp(12), dp(6)]
                            md_bg_color: root.icon_bg_color_rgba  # Dynamic color
                            size_hint: None, None
                            size: self.minimum_width, dp(32)
                            
                            MDLabel:
                                text: root.category_name
                                font_style: "Label"
                                role: "large"
                                theme_text_color: "Custom"
                                text_color: root.icon_color_rgba  # Dynamic color
                                adaptive_size: True
                
                # Details Card
                MDCard:
                    style: "elevated"
                    radius: [dp(16), dp(16), dp(16), dp(16)]
                    padding: dp(16)
                    md_bg_color: app.theme_cls.surfaceContainerColor
                    orientation: 'vertical'
                    adaptive_height: True
                    spacing: dp(4)
                    
                    DetailRow:
                        row_icon: "calendar"
                        row_label: "Date"
                        row_value: root.date_text
                        
                    MDDivider:
                        
                    DetailRow:
                        row_icon: "clock-outline"
                        row_label: "Added"
                        row_value: root.created_at_text
                
                # Note Card (if exists)
                MDCard:
                    id: note_card
                    style: "elevated"
                    radius: [dp(16), dp(16), dp(16), dp(16)]
                    padding: dp(16)
                    md_bg_color: app.theme_cls.surfaceContainerColor
                    orientation: 'vertical'
                    adaptive_height: True
                    spacing: dp(8)
                    opacity: 1 if root.note_text else 0
                    size_hint_y: None
                    height: self.minimum_height if root.note_text else 0
                    
                    MDBoxLayout:
                        adaptive_height: True
                        spacing: dp(8)
                        
                        MDIcon:
                            icon: "note-text"
                            theme_icon_color: "Custom"
                            icon_color: [0.0, 0.737, 0.831, 0.7]  # #00BCD4 with 0.7 alpha
                            
                        MDLabel:
                            text: "Note"
                            font_style: "Label"
                            role: "large"
                            theme_text_color: "Secondary"
                            adaptive_height: True
                    
                    MDLabel:
                        text: root.note_text
                        font_style: "Body"
                        role: "medium"
                        theme_text_color: "Primary"
                        adaptive_height: True
                
                # Receipt Image Card (if exists)
                MDCard:
                    id: receipt_card
                    style: "elevated"
                    radius: [dp(16), dp(16), dp(16), dp(16)]
                    padding: dp(16)
                    md_bg_color: app.theme_cls.surfaceContainerColor
                    orientation: 'vertical'
                    adaptive_height: True
                    spacing: dp(12)
                    opacity: 1 if root.has_receipt_image else 0
                    size_hint_y: None
                    height: self.minimum_height if root.has_receipt_image else 0
                    
                    MDBoxLayout:
                        adaptive_height: True
                        spacing: dp(8)
                        
                        MDIcon:
                            icon: "image"
                            theme_icon_color: "Custom"
                            icon_color: [0.0, 0.737, 0.831, 0.7]  # #00BCD4 with 0.7 alpha
                            
                        MDLabel:
                            text: "Receipt Image"
                            font_style: "Label"
                            role: "large"
                            theme_text_color: "Secondary"
                            adaptive_height: True
                    
                    FitImage:
                        id: receipt_image
                        source: root.receipt_image_path
                        size_hint_y: None
                        height: dp(200)
                        radius: [dp(12), dp(12), dp(12), dp(12)]
                
                Widget:
                    size_hint_y: None
                    height: dp(32)
''')


class ExpenseDetailScreen(MDScreen):
    """Screen showing expense details"""
    
    expense_id = NumericProperty(0)
    store_name = StringProperty("")
    amount_text = StringProperty("€0.00")
    category_name = StringProperty("")
    category_icon = StringProperty("receipt")
    category_color = StringProperty("#00BCD4")
    icon_color_rgba = StringProperty([0.0, 0.737, 0.831, 1.0])  # Default #00BCD4
    icon_bg_color_rgba = StringProperty([0.0, 0.737, 0.831, 0.15])  # Default #00BCD4 with 0.15 alpha
    date_text = StringProperty("")
    created_at_text = StringProperty("")
    note_text = StringProperty("")
    receipt_image_path = StringProperty("")
    has_receipt_image = BooleanProperty(False)
    
    def load_expense(self, expense_id):
        """Load expense data from database"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        db = app.db
        
        self.expense_id = expense_id
        expense = db.get_expense(expense_id)
        
        if not expense:
            app.show_snackbar("Expense not found")
            app.go_back()
            return
        
        # Set basic info
        self.store_name = expense['store_name']
        self.amount_text = f"€{expense['amount']:,.2f}"
        
        # Get category info
        category_info = db.get_category_info(expense['category'])
        self.category_name = category_info['name']
        self.category_icon = category_info['icon']
        
        # Convert hex color to RGBA
        rgba = list(get_color_from_hex(category_info['color']))
        self.icon_color_rgba = rgba
        self.icon_bg_color_rgba = rgba[:3] + [0.15]  # Same color with 0.15 alpha
        
        # Format date
        try:
            date_obj = datetime.strptime(expense['date'], '%Y-%m-%d')
            self.date_text = date_obj.strftime('%B %d, %Y')
        except:
            self.date_text = expense['date']
        
        # Format created at
        try:
            created_obj = datetime.fromisoformat(expense['created_at'])
            self.created_at_text = created_obj.strftime('%b %d, %Y at %H:%M')
        except:
            self.created_at_text = expense.get('created_at', 'Unknown')
        
        # Note
        self.note_text = expense.get('note', '') or ''
        
        # Receipt image
        receipt_path = expense.get('receipt_image', '')
        if receipt_path and os.path.exists(receipt_path):
            self.receipt_image_path = receipt_path
            self.has_receipt_image = True
        else:
            self.receipt_image_path = ''
            self.has_receipt_image = False
    
    def edit_expense(self):
        """Navigate to edit expense screen"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        db = app.db
        
        expense = db.get_expense(self.expense_id)
        if expense:
            # Prefill the add expense form with current data
            prefill_data = {
                'amount': expense['amount'],
                'store_name': expense['store_name'],
                'date': expense['date'],
                'category': expense['category'],
                'note': expense.get('note', ''),
                'receipt_image': expense.get('receipt_image', '')
            }
            
            # For now, we'll delete and re-add (simple edit)
            # In a full implementation, we'd have a proper edit mode
            app.go_to_add_expense(prefill_data)
            
            # Delete the old expense
            db.delete_expense(self.expense_id)
    
    def confirm_delete(self):
        """Show delete confirmation dialog"""
        self.dialog = MDDialog(
            MDDialogHeadlineText(text="Delete Expense?"),
            MDDialogSupportingText(
                text=f"Are you sure you want to delete this expense from {self.store_name}? This action cannot be undone."
            ),
            MDDialogButtonContainer(
                MDButton(
                    MDButtonText(text="Cancel"),
                    style="text",
                    on_release=lambda x: self.dialog.dismiss()
                ),
                MDButton(
                    MDButtonText(text="Delete"),
                    style="text",
                    on_release=lambda x: self.delete_expense()
                ),
                spacing="8dp",
            ),
        )
        self.dialog.open()
    
    def delete_expense(self):
        """Delete the expense"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        db = app.db
        
        self.dialog.dismiss()
        
        db.delete_expense(self.expense_id)
        app.show_snackbar("Expense deleted")
        app.go_back()
