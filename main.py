"""
Pocket AI - Futuristic Expense & Receipt Tracker
An offline-first Android app with advanced OCR capabilities
"""

from kivy.config import Config
Config.set('graphics', 'width', '400')
Config.set('graphics', 'height', '800')

from kivymd.app import MDApp
from kivy.lang import Builder
from kivy.core.window import Window
from kivy.utils import platform
from kivy.clock import Clock
from kivy.properties import StringProperty, NumericProperty, ObjectProperty
from kivy.uix.screenmanager import ScreenManager, SlideTransition, FadeTransition

from kivymd.uix.screen import MDScreen
from kivymd.uix.card import MDCard
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.floatlayout import MDFloatLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.button import MDIconButton, MDFabButton
from kivymd.uix.navigationbar import MDNavigationBar, MDNavigationItem
from kivymd.uix.snackbar import MDSnackbar, MDSnackbarText

import os
import sys

# Add the app directory to path
if platform == 'android':
    from android.storage import app_storage_path
    APP_PATH = app_storage_path()
else:
    APP_PATH = os.path.dirname(os.path.abspath(__file__))

sys.path.insert(0, APP_PATH)

from database.db_manager import DatabaseManager
from screens.dashboard import DashboardScreen
from screens.scan import ScanScreen
from screens.receipts import ReceiptsScreen
from screens.stats import StatsScreen
from screens.expense_detail import ExpenseDetailScreen
from screens.add_expense import AddExpenseScreen

# Set window size for desktop testing
if platform != 'android':
    Window.size = (400, 800)

KV = '''
#:import FadeTransition kivy.uix.screenmanager.FadeTransition
#:import SlideTransition kivy.uix.screenmanager.SlideTransition
#:import utils kivy.utils

MDBoxLayout:
    orientation: 'vertical'
    md_bg_color: app.theme_cls.backgroundColor
    
    ScreenManager:
        id: screen_manager
        transition: FadeTransition(duration=0.2)
        
        DashboardScreen:
            name: 'dashboard'
            
        ScanScreen:
            name: 'scan'
            
        ReceiptsScreen:
            name: 'receipts'
            
        StatsScreen:
            name: 'stats'
            
        ExpenseDetailScreen:
            name: 'expense_detail'
            
        AddExpenseScreen:
            name: 'add_expense'
    
    MDNavigationBar:
        id: bottom_nav
        on_switch_tabs: app.on_tab_switch(*args)
        
        MDNavigationItem:
            icon: 'view-dashboard'
            text: 'Dashboard'
            active: True
            
        MDNavigationItem:
            icon: 'camera'
            text: 'Scan'
            
        MDNavigationItem:
            icon: 'receipt'
            text: 'Receipts'
            
        MDNavigationItem:
            icon: 'chart-bar'
            text: 'Stats'
'''


class PocketAIApp(MDApp):
    """Main application class for Pocket AI"""
    
    db = ObjectProperty(None)
    current_expense_id = NumericProperty(0)
    
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.db = DatabaseManager()
        # Initialize database immediately
        self.db.init_db()
        
    def build(self):
        self.theme_cls.theme_style = "Dark"
        self.theme_cls.primary_palette = "Cyan"
        self.title = "Pocket AI"
        
        return Builder.load_string(KV)
    
    def on_start(self):
        """Called when the app starts"""
        # Initialize database
        self.db.init_db()
        
        # Refresh dashboard data
        Clock.schedule_once(lambda dt: self.refresh_dashboard(), 0.5)
    
    def on_tab_switch(self, bar, item, item_icon, item_text):
        """Handle bottom navigation tab switches"""
        screen_map = {
            'Dashboard': 'dashboard',
            'Scan': 'scan',
            'Receipts': 'receipts',
            'Stats': 'stats'
        }
        
        screen_name = screen_map.get(item_text, 'dashboard')
        self.root.ids.screen_manager.current = screen_name
        
        # Refresh data when switching to certain screens
        if screen_name == 'dashboard':
            self.refresh_dashboard()
        elif screen_name == 'receipts':
            self.refresh_receipts()
        elif screen_name == 'stats':
            self.refresh_stats()
    
    def refresh_dashboard(self):
        """Refresh dashboard screen data"""
        dashboard = self.root.ids.screen_manager.get_screen('dashboard')
        dashboard.refresh_data()
    
    def refresh_receipts(self):
        """Refresh receipts list"""
        receipts = self.root.ids.screen_manager.get_screen('receipts')
        receipts.refresh_data()
    
    def refresh_stats(self):
        """Refresh stats screen"""
        stats = self.root.ids.screen_manager.get_screen('stats')
        stats.refresh_data()
    
    def go_to_expense_detail(self, expense_id):
        """Navigate to expense detail screen"""
        self.current_expense_id = expense_id
        detail_screen = self.root.ids.screen_manager.get_screen('expense_detail')
        detail_screen.load_expense(expense_id)
        self.root.ids.screen_manager.transition = SlideTransition(direction='left')
        self.root.ids.screen_manager.current = 'expense_detail'
    
    def go_to_add_expense(self, prefill_data=None):
        """Navigate to add expense screen"""
        add_screen = self.root.ids.screen_manager.get_screen('add_expense')
        add_screen.reset_form(prefill_data)
        self.root.ids.screen_manager.transition = SlideTransition(direction='left')
        self.root.ids.screen_manager.current = 'add_expense'
    
    def go_back(self):
        """Navigate back to previous screen"""
        self.root.ids.screen_manager.transition = SlideTransition(direction='right')
        self.root.ids.screen_manager.current = 'receipts'
    
    def show_snackbar(self, text):
        """Show a snackbar message"""
        MDSnackbar(
            MDSnackbarText(text=text),
            y="24dp",
            pos_hint={"center_x": 0.5},
            size_hint_x=0.9,
        ).open()


if __name__ == '__main__':
    PocketAIApp().run()
