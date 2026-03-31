"""
Receipts Screen for Pocket AI
Shows list of all saved expenses/receipts
"""

from kivy.lang import Builder
from kivy.properties import StringProperty, NumericProperty, ListProperty, BooleanProperty
from kivy.clock import Clock
from kivy.metrics import dp
from kivy.utils import get_color_from_hex

from kivymd.uix.screen import MDScreen
from kivymd.uix.card import MDCard
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.button import MDIconButton, MDButton, MDButtonText
from kivymd.uix.textfield import MDTextField
from kivymd.uix.list import MDListItem

from datetime import datetime

Builder.load_string('''
<ReceiptCard>:
    style: "elevated"
    radius: [dp(16), dp(16), dp(16), dp(16)]
    padding: dp(16)
    md_bg_color: app.theme_cls.surfaceContainerColor
    orientation: 'vertical'
    adaptive_height: True
    spacing: dp(12)
    on_release: app.go_to_expense_detail(root.expense_id)
    ripple_behavior: True
    
    MDBoxLayout:
        adaptive_height: True
        spacing: dp(12)
        
        # Store Logo/Icon
        MDCard:
            style: "filled"
            size_hint: None, None
            size: dp(48), dp(48)
            radius: [dp(12), dp(12), dp(12), dp(12)]
            md_bg_color: root.icon_bg_color
            padding: dp(8)
            
            MDIcon:
                icon: root.category_icon
                halign: "center"
                valign: "center"
                theme_icon_color: "Custom"
                icon_color: root.icon_color
                font_size: dp(24)
        
        # Store Info
        MDBoxLayout:
            orientation: 'vertical'
            adaptive_height: True
            spacing: dp(2)
            
            MDLabel:
                text: root.store_name
                font_style: "Title"
                role: "medium"
                theme_text_color: "Primary"
                adaptive_height: True
                bold: True
                
            MDLabel:
                text: root.category_name
                font_style: "Body"
                role: "small"
                theme_text_color: "Secondary"
                adaptive_height: True
        
        # Amount
        MDBoxLayout:
            orientation: 'vertical'
            adaptive_height: True
            size_hint_x: None
            width: dp(80)
            
            MDLabel:
                text: root.amount_text
                font_style: "Title"
                role: "medium"
                theme_text_color: "Custom"
                text_color: 0, 0.74, 0.83, 1
                adaptive_height: True
                halign: "right"
                bold: True
                
            MDLabel:
                text: root.date_text
                font_style: "Body"
                role: "small"
                theme_text_color: "Secondary"
                adaptive_height: True
                halign: "right"
    
    # Note preview if exists
    MDLabel:
        text: root.note_preview
        font_style: "Body"
        role: "small"
        theme_text_color: "Secondary"
        adaptive_height: True
        opacity: 1 if root.note_preview else 0
        size_hint_y: None
        height: dp(20) if root.note_preview else 0

<ReceiptsScreen>:
    name: 'receipts'
    md_bg_color: app.theme_cls.backgroundColor
    
    MDBoxLayout:
        orientation: 'vertical'
        
        # Header
        MDBoxLayout:
            adaptive_height: True
            padding: [dp(16), dp(16), dp(16), dp(8)]
            spacing: dp(12)
            
            MDBoxLayout:
                orientation: 'vertical'
                adaptive_height: True
                
                MDLabel:
                    text: "Receipts"
                    font_style: "Headline"
                    role: "medium"
                    theme_text_color: "Primary"
                    adaptive_height: True
                    bold: True
                    
                MDLabel:
                    text: root.subtitle_text
                    font_style: "Body"
                    role: "medium"
                    theme_text_color: "Secondary"
                    adaptive_height: True
            
            MDIconButton:
                icon: "magnify"
                style: "standard"
                on_release: root.toggle_search()
                
            MDIconButton:
                icon: "plus-circle"
                style: "tonal"
                theme_icon_color: "Custom"
                icon_color: 0, 0.74, 0.83, 1
                on_release: app.go_to_add_expense()
        
        # Search Bar (hidden by default)
        MDBoxLayout:
            id: search_container
            adaptive_height: True
            padding: [dp(16), 0, dp(16), dp(8)]
            opacity: 0
            disabled: True
            size_hint_y: None
            height: 0
            
            MDTextField:
                id: search_field
                mode: "outlined"
                hint_text: "Search receipts..."
                on_text: root.search_expenses(self.text)
                
                MDTextFieldLeadingIcon:
                    icon: "magnify"
                    
                MDIconButton:
                    icon: "close"
                    on_release: root.clear_search()
        
        # Receipts List
        MDScrollView:
            do_scroll_x: False
            
            MDBoxLayout:
                id: receipts_container
                orientation: 'vertical'
                padding: dp(16)
                spacing: dp(12)
                adaptive_height: True
        
        # Empty State
        MDBoxLayout:
            id: empty_state
            orientation: 'vertical'
            padding: [dp(32), dp(64), dp(32), dp(64)]
            spacing: dp(16)
            opacity: 0
            
            MDIcon:
                icon: "receipt-text-outline"
                halign: "center"
                font_size: dp(80)
                theme_icon_color: "Custom"
                icon_color: app.theme_cls.onSurfaceVariantColor
                
            MDLabel:
                text: "No receipts yet"
                font_style: "Headline"
                role: "small"
                theme_text_color: "Secondary"
                adaptive_height: True
                halign: "center"
                
            MDLabel:
                text: "Start tracking your expenses by\\nadding a receipt or scanning one"
                font_style: "Body"
                role: "medium"
                theme_text_color: "Secondary"
                adaptive_height: True
                halign: "center"
                
            Widget:
                size_hint_y: None
                height: dp(16)
                
            MDButton:
                style: "filled"
                pos_hint: {"center_x": 0.5}
                on_release: app.go_to_add_expense()
                
                MDButtonText:
                    text: "Add Your First Receipt"
                    
            MDButton:
                style: "outlined"
                pos_hint: {"center_x": 0.5}
                on_release: app.root.ids.screen_manager.current = 'scan'
                
                MDButtonText:
                    text: "Scan a Receipt"
''')


class ReceiptCard(MDCard):
    """Card displaying a receipt/expense"""
    expense_id = NumericProperty(0)
    store_name = StringProperty("")
    category_name = StringProperty("")
    category_icon = StringProperty("receipt")
    category_color = StringProperty("#00BCD4")
    icon_color = ListProperty([0, 0.74, 0.83, 1])
    icon_bg_color = ListProperty([0, 0.74, 0.83, 0.15])
    amount_text = StringProperty("€0.00")
    date_text = StringProperty("")
    note_preview = StringProperty("")


class ReceiptsScreen(MDScreen):
    """Screen showing all receipts/expenses"""
    
    subtitle_text = StringProperty("All your expenses")
    search_visible = BooleanProperty(False)
    
    def on_enter(self):
        """Called when screen is displayed"""
        Clock.schedule_once(lambda dt: self.refresh_data(), 0.1)
    
    def refresh_data(self):
        """Refresh receipts list from database"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        db = app.db
        
        container = self.ids.receipts_container
        container.clear_widgets()
        
        expenses = db.get_all_expenses()
        
        # Update subtitle
        total = sum(e['amount'] for e in expenses)
        self.subtitle_text = f"{len(expenses)} receipts • €{total:,.2f} total"
        
        # Show/hide empty state
        if not expenses:
            self.ids.empty_state.opacity = 1
            return
        else:
            self.ids.empty_state.opacity = 0
        
        for expense in expenses:
            self.add_receipt_card(expense, container, db)
    
    def add_receipt_card(self, expense, container, db):
        """Add a receipt card to the container"""
        category_info = db.get_category_info(expense['category'])
        
        # Format date
        try:
            date_obj = datetime.strptime(expense['date'], '%Y-%m-%d')
            date_text = date_obj.strftime('%b %d')
        except:
            date_text = expense['date']
        
        # Truncate note for preview
        note_preview = ""
        if expense.get('note'):
            note = expense['note']
            note_preview = note[:50] + "..." if len(note) > 50 else note
        
        # Convert hex color to RGBA
        hex_color = category_info['color']
        rgba = list(get_color_from_hex(hex_color))
        bg_rgba = rgba[:3] + [0.15]  # Same color with low alpha for background
        
        card = ReceiptCard(
            expense_id=expense['id'],
            store_name=expense['store_name'],
            category_name=category_info['name'],
            category_icon=category_info['icon'],
            category_color=category_info['color'],
            icon_color=rgba,
            icon_bg_color=bg_rgba,
            amount_text=f"€{expense['amount']:,.2f}",
            date_text=date_text,
            note_preview=note_preview
        )
        container.add_widget(card)
    
    def toggle_search(self):
        """Toggle search bar visibility"""
        from kivy.animation import Animation
        
        search_container = self.ids.search_container
        
        if self.search_visible:
            # Hide search
            anim = Animation(opacity=0, height=0, duration=0.2)
            anim.start(search_container)
            search_container.disabled = True
            self.search_visible = False
            self.clear_search()
        else:
            # Show search
            search_container.disabled = False
            anim = Animation(opacity=1, height=dp(70), duration=0.2)
            anim.start(search_container)
            self.search_visible = True
            self.ids.search_field.focus = True
    
    def search_expenses(self, query):
        """Search expenses by query"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        db = app.db
        
        container = self.ids.receipts_container
        container.clear_widgets()
        
        if query:
            expenses = db.search_expenses(query)
        else:
            expenses = db.get_all_expenses()
        
        # Update subtitle
        if query:
            self.subtitle_text = f"Found {len(expenses)} results"
        else:
            total = sum(e['amount'] for e in expenses)
            self.subtitle_text = f"{len(expenses)} receipts • €{total:,.2f} total"
        
        # Show/hide empty state
        if not expenses:
            self.ids.empty_state.opacity = 1
            return
        else:
            self.ids.empty_state.opacity = 0
        
        for expense in expenses:
            self.add_receipt_card(expense, container, db)
    
    def clear_search(self):
        """Clear search and show all receipts"""
        self.ids.search_field.text = ""
        self.refresh_data()
