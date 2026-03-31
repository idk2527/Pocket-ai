"""
Stats Screen for Pocket AI
Shows spending statistics and category breakdowns
"""

from kivy.lang import Builder
from kivy.properties import StringProperty, NumericProperty, ListProperty, BooleanProperty
from kivy.clock import Clock
from kivy.metrics import dp
from kivy.utils import get_color_from_hex
from kivy.graphics import Color, RoundedRectangle, Line, Ellipse

from kivymd.uix.screen import MDScreen
from kivymd.uix.card import MDCard
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.button import MDIconButton, MDButton, MDButtonText
from kivymd.uix.progressindicator import MDLinearProgressIndicator

from datetime import datetime, timedelta

Builder.load_string('''
<CategoryStatCard>:
    style: "elevated"
    radius: [dp(16), dp(16), dp(16), dp(16)]
    padding: dp(16)
    md_bg_color: app.theme_cls.surfaceContainerColor
    orientation: 'vertical'
    adaptive_height: True
    spacing: dp(12)
    
    MDBoxLayout:
        adaptive_height: True
        spacing: dp(12)
        
        # Category Icon
        MDCard:
            style: "filled"
            size_hint: None, None
            size: dp(40), dp(40)
            radius: [dp(10), dp(10), dp(10), dp(10)]
            md_bg_color: root.icon_bg_color
            padding: dp(6)
            
            MDIcon:
                icon: root.category_icon
                halign: "center"
                valign: "center"
                theme_icon_color: "Custom"
                icon_color: root.icon_color
                font_size: dp(20)
        
        # Category Info
        MDBoxLayout:
            orientation: 'vertical'
            adaptive_height: True
            spacing: dp(2)
            
            MDLabel:
                text: root.category_name
                font_style: "Title"
                role: "small"
                theme_text_color: "Primary"
                adaptive_height: True
                bold: True
                
            MDLabel:
                text: root.expense_count
                font_style: "Body"
                role: "small"
                theme_text_color: "Secondary"
                adaptive_height: True
        
        # Amount
        MDLabel:
            text: root.amount_text
            font_style: "Title"
            role: "medium"
            theme_text_color: "Custom"
            text_color: root.icon_color
            adaptive_height: True
            halign: "right"
            bold: True
            size_hint_x: None
            width: dp(100)
    
    # Progress Bar
    MDBoxLayout:
        adaptive_height: True
        padding: [0, dp(4), 0, 0]
        
        MDLinearProgressIndicator:
            value: root.percentage
            size_hint_y: None
            height: dp(6)
            radius: [dp(3), dp(3), dp(3), dp(3)]
            color: root.icon_color

<PeriodButton>:
    style: "outlined" if not root.is_active else "filled"
    
    MDButtonText:
        text: root.period_text
        theme_text_color: "Custom"
        text_color: 0, 0.74, 0.83, 1

<StatsScreen>:
    name: 'stats'
    md_bg_color: app.theme_cls.backgroundColor
    
    MDScrollView:
        do_scroll_x: False
        
        MDBoxLayout:
            orientation: 'vertical'
            padding: dp(16)
            spacing: dp(16)
            adaptive_height: True
            
            # Header
            MDBoxLayout:
                adaptive_height: True
                padding: [0, dp(8), 0, dp(8)]
                
                MDBoxLayout:
                    orientation: 'vertical'
                    adaptive_height: True
                    
                    MDLabel:
                        text: "Statistics"
                        font_style: "Headline"
                        role: "medium"
                        theme_text_color: "Primary"
                        adaptive_height: True
                        bold: True
                        
                    MDLabel:
                        text: root.period_label
                        font_style: "Body"
                        role: "medium"
                        theme_text_color: "Secondary"
                        adaptive_height: True
            
            # Period Selector
            MDBoxLayout:
                adaptive_height: True
                spacing: dp(8)
                
                PeriodButton:
                    period_text: "7 Days"
                    is_active: root.selected_period == '7d'
                    on_release: root.set_period('7d')
                    
                PeriodButton:
                    period_text: "30 Days"
                    is_active: root.selected_period == '30d'
                    on_release: root.set_period('30d')
                    
                PeriodButton:
                    period_text: "This Month"
                    is_active: root.selected_period == 'month'
                    on_release: root.set_period('month')
                    
                PeriodButton:
                    period_text: "All Time"
                    is_active: root.selected_period == 'all'
                    on_release: root.set_period('all')
            
            # Total Spending Card
            MDCard:
                style: "elevated"
                radius: [dp(20), dp(20), dp(20), dp(20)]
                padding: dp(24)
                md_bg_color: app.theme_cls.surfaceContainerColor
                orientation: 'vertical'
                adaptive_height: True
                spacing: dp(8)
                
                MDLabel:
                    text: "Total Spending"
                    font_style: "Title"
                    role: "medium"
                    theme_text_color: "Secondary"
                    adaptive_height: True
                    halign: "center"
                    
                MDLabel:
                    text: root.total_spending
                    font_style: "Display"
                    role: "small"
                    theme_text_color: "Custom"
                    text_color: 0, 0.74, 0.83, 1
                    adaptive_height: True
                    halign: "center"
                    bold: True
                    
                MDBoxLayout:
                    adaptive_height: True
                    spacing: dp(24)
                    padding: [0, dp(8), 0, 0]
                    
                    MDBoxLayout:
                        orientation: 'vertical'
                        adaptive_height: True
                        
                        MDLabel:
                            text: root.expense_count
                            font_style: "Headline"
                            role: "small"
                            theme_text_color: "Primary"
                            adaptive_height: True
                            halign: "center"
                            bold: True
                            
                        MDLabel:
                            text: "Expenses"
                            font_style: "Body"
                            role: "small"
                            theme_text_color: "Secondary"
                            adaptive_height: True
                            halign: "center"
                    
                    MDBoxLayout:
                        orientation: 'vertical'
                        adaptive_height: True
                        
                        MDLabel:
                            text: root.avg_expense
                            font_style: "Headline"
                            role: "small"
                            theme_text_color: "Primary"
                            adaptive_height: True
                            halign: "center"
                            bold: True
                            
                        MDLabel:
                            text: "Average"
                            font_style: "Body"
                            role: "small"
                            theme_text_color: "Secondary"
                            adaptive_height: True
                            halign: "center"
                    
                    MDBoxLayout:
                        orientation: 'vertical'
                        adaptive_height: True
                        
                        MDLabel:
                            text: root.daily_avg
                            font_style: "Headline"
                            role: "small"
                            theme_text_color: "Primary"
                            adaptive_height: True
                            halign: "center"
                            bold: True
                            
                        MDLabel:
                            text: "Daily Avg"
                            font_style: "Body"
                            role: "small"
                            theme_text_color: "Secondary"
                            adaptive_height: True
                            halign: "center"
            
            # Category Breakdown Header
            MDBoxLayout:
                adaptive_height: True
                padding: [0, dp(8), 0, 0]
                
                MDLabel:
                    text: "By Category"
                    font_style: "Title"
                    role: "large"
                    theme_text_color: "Primary"
                    adaptive_height: True
                    bold: True
            
            # Category Stats Container
            MDBoxLayout:
                id: category_stats_container
                orientation: 'vertical'
                adaptive_height: True
                spacing: dp(12)
            
            # Empty State
            MDBoxLayout:
                id: empty_state
                orientation: 'vertical'
                adaptive_height: True
                padding: [0, dp(32), 0, dp(32)]
                spacing: dp(16)
                opacity: 0
                
                MDIcon:
                    icon: "chart-bar"
                    halign: "center"
                    font_size: dp(64)
                    theme_icon_color: "Custom"
                    icon_color: app.theme_cls.onSurfaceVariantColor
                    
                MDLabel:
                    text: "No data yet"
                    font_style: "Title"
                    role: "medium"
                    theme_text_color: "Secondary"
                    adaptive_height: True
                    halign: "center"
                    
                MDLabel:
                    text: "Add some expenses to see\\nyour spending statistics"
                    font_style: "Body"
                    role: "medium"
                    theme_text_color: "Secondary"
                    adaptive_height: True
                    halign: "center"
''')


class CategoryStatCard(MDCard):
    """Card displaying category statistics"""
    category_name = StringProperty("")
    category_icon = StringProperty("receipt")
    category_color = StringProperty("#00BCD4")
    icon_color = ListProperty([0, 0.74, 0.83, 1])
    icon_bg_color = ListProperty([0, 0.74, 0.83, 0.15])
    amount_text = StringProperty("€0.00")
    expense_count = StringProperty("0 expenses")
    percentage = NumericProperty(0)


class PeriodButton(MDButton):
    """Button for selecting time period"""
    period_text = StringProperty("")
    is_active = BooleanProperty(False)


class StatsScreen(MDScreen):
    """Screen showing spending statistics"""
    
    selected_period = StringProperty('month')
    period_label = StringProperty("This Month")
    total_spending = StringProperty("€0.00")
    expense_count = StringProperty("0")
    avg_expense = StringProperty("€0.00")
    daily_avg = StringProperty("€0.00")
    
    def on_enter(self):
        """Called when screen is displayed"""
        Clock.schedule_once(lambda dt: self.refresh_data(), 0.1)
    
    def set_period(self, period):
        """Set the selected time period"""
        self.selected_period = period
        
        period_labels = {
            '7d': 'Last 7 Days',
            '30d': 'Last 30 Days',
            'month': 'This Month',
            'all': 'All Time'
        }
        self.period_label = period_labels.get(period, 'This Month')
        
        self.refresh_data()
    
    def get_date_range(self):
        """Get start and end dates based on selected period"""
        now = datetime.now()
        end_date = now.strftime('%Y-%m-%d')
        
        if self.selected_period == '7d':
            start_date = (now - timedelta(days=7)).strftime('%Y-%m-%d')
            days = 7
        elif self.selected_period == '30d':
            start_date = (now - timedelta(days=30)).strftime('%Y-%m-%d')
            days = 30
        elif self.selected_period == 'month':
            start_date = now.replace(day=1).strftime('%Y-%m-%d')
            days = now.day
        else:  # all time
            start_date = None
            end_date = None
            days = None
        
        return start_date, end_date, days
    
    def refresh_data(self):
        """Refresh statistics from database"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        db = app.db
        
        start_date, end_date, days = self.get_date_range()
        
        # Get total spending
        total = db.get_total_spending(start_date, end_date)
        self.total_spending = f"€{total:,.2f}"
        
        # Get expense count
        count = db.get_expense_count(start_date, end_date)
        self.expense_count = str(count)
        
        # Calculate average expense
        if count > 0:
            avg = total / count
            self.avg_expense = f"€{avg:,.2f}"
        else:
            self.avg_expense = "€0.00"
        
        # Calculate daily average
        if days and days > 0:
            daily = total / days
            self.daily_avg = f"€{daily:,.2f}"
        elif self.selected_period == 'all' and count > 0:
            # For all time, calculate based on first expense date
            expenses = db.get_all_expenses()
            if expenses:
                try:
                    first_date = datetime.strptime(expenses[-1]['date'], '%Y-%m-%d')
                    days_total = (datetime.now() - first_date).days + 1
                    daily = total / days_total if days_total > 0 else 0
                    self.daily_avg = f"€{daily:,.2f}"
                except:
                    self.daily_avg = "€0.00"
            else:
                self.daily_avg = "€0.00"
        else:
            self.daily_avg = "€0.00"
        
        # Load category breakdown
        self.load_category_stats(start_date, end_date, total)
    
    def load_category_stats(self, start_date, end_date, total_spending):
        """Load category statistics"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        db = app.db
        
        container = self.ids.category_stats_container
        container.clear_widgets()
        
        category_totals = db.get_category_totals(start_date, end_date)
        
        # Show/hide empty state
        if not category_totals:
            self.ids.empty_state.opacity = 1
            return
        else:
            self.ids.empty_state.opacity = 0
        
        for cat_data in category_totals:
            category_info = db.get_category_info(cat_data['category'])
            
            # Calculate percentage
            percentage = (cat_data['total'] / total_spending * 100) if total_spending > 0 else 0
            
            # Convert hex color to RGBA
            hex_color = category_info['color']
            rgba = list(get_color_from_hex(hex_color))
            bg_rgba = rgba[:3] + [0.15]
            
            card = CategoryStatCard(
                category_name=category_info['name'],
                category_icon=category_info['icon'],
                category_color=category_info['color'],
                icon_color=rgba,
                icon_bg_color=bg_rgba,
                amount_text=f"€{cat_data['total']:,.2f}",
                expense_count=f"{cat_data['count']} expense{'s' if cat_data['count'] != 1 else ''}",
                percentage=percentage
            )
            container.add_widget(card)
