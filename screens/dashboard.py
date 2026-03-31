"""
Dashboard Screen for Pocket AI
Shows spending overview, recent expenses, and quick actions
"""

from kivy.lang import Builder
from kivy.properties import StringProperty, NumericProperty, ListProperty
from kivy.clock import Clock
from kivy.metrics import dp
from kivy.utils import get_color_from_hex

from kivymd.uix.screen import MDScreen
from kivymd.uix.card import MDCard
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.label import MDLabel
from kivymd.uix.button import MDIconButton, MDButton, MDButtonText
from kivymd.uix.list import MDList, MDListItem, MDListItemHeadlineText, MDListItemSupportingText, MDListItemLeadingIcon, MDListItemTertiaryText

from datetime import datetime, timedelta

Builder.load_string('''
#:import get_color_from_hex kivy.utils.get_color_from_hex

<GlowCard@MDCard>:
    style: "elevated"
    radius: [dp(16), dp(16), dp(16), dp(16)]
    padding: dp(16)
    md_bg_color: app.theme_cls.surfaceContainerColor

<StatCard>:
    orientation: 'vertical'
    adaptive_height: True
    padding: dp(16)
    spacing: dp(8)
    style: "elevated"
    radius: [dp(20), dp(20), dp(20), dp(20)]
    md_bg_color: app.theme_cls.surfaceContainerColor
    
    MDBoxLayout:
        adaptive_height: True
        spacing: dp(8)
        
        MDIcon:
            icon: root.icon
            theme_icon_color: "Custom"
            icon_color: root.glow_color
            pos_hint: {"center_y": 0.5}
            
        MDLabel:
            text: root.title
            font_style: "Label"
            role: "medium"
            theme_text_color: "Secondary"
            adaptive_height: True
    
    MDLabel:
        text: root.value
        font_style: "Headline"
        role: "small"
        theme_text_color: "Primary"
        adaptive_height: True
        bold: True
    
    MDLabel:
        text: root.subtitle
        font_style: "Body"
        role: "small"
        theme_text_color: "Secondary"
        adaptive_height: True

<RecentExpenseItem>:
    on_release: app.go_to_expense_detail(root.expense_id)
    theme_bg_color: "Custom"
    md_bg_color: app.theme_cls.surfaceContainerColor
    radius: [dp(12), dp(12), dp(12), dp(12)]
    
    MDListItemLeadingIcon:
        icon: root.category_icon
        theme_icon_color: "Custom"
        icon_color: root.icon_color
        
    MDListItemHeadlineText:
        text: root.store_name
        
    MDListItemSupportingText:
        text: root.date_text
        
    MDListItemTertiaryText:
        text: root.amount_text
        theme_text_color: "Custom"
        text_color: 0, 0.74, 0.83, 1

<DashboardScreen>:
    name: 'dashboard'
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
                padding: [0, dp(8), 0, dp(16)]
                
                MDBoxLayout:
                    orientation: 'vertical'
                    adaptive_height: True
                    spacing: dp(4)
                    
                    MDLabel:
                        text: "Pocket AI"
                        font_style: "Headline"
                        role: "medium"
                        theme_text_color: "Primary"
                        adaptive_height: True
                        bold: True
                        
                    MDLabel:
                        text: root.greeting_text
                        font_style: "Body"
                        role: "medium"
                        theme_text_color: "Secondary"
                        adaptive_height: True
                
                MDIconButton:
                    icon: "plus-circle"
                    style: "tonal"
                    theme_icon_color: "Custom"
                    icon_color: 0, 0.74, 0.83, 1
                    on_release: app.go_to_add_expense()
            
            # Stats Cards Row
            MDBoxLayout:
                adaptive_height: True
                spacing: dp(12)
                
                StatCard:
                    title: "This Month"
                    value: root.month_total
                    subtitle: root.month_subtitle
                    icon: "calendar-month"
                    glow_color: 0, 0.74, 0.83, 1
                    size_hint_x: 0.5
                    
                StatCard:
                    title: "Last 7 Days"
                    value: root.week_total
                    subtitle: root.week_subtitle
                    icon: "calendar-week"
                    glow_color: 0.91, 0.12, 0.39, 1
                    size_hint_x: 0.5
            
            # Quick Stats
            GlowCard:
                orientation: 'horizontal'
                adaptive_height: True
                spacing: dp(16)
                
                MDBoxLayout:
                    orientation: 'vertical'
                    adaptive_height: True
                    size_hint_x: 0.5
                    
                    MDLabel:
                        text: root.total_expenses
                        font_style: "Headline"
                        role: "small"
                        theme_text_color: "Primary"
                        adaptive_height: True
                        bold: True
                        halign: "center"
                        
                    MDLabel:
                        text: "Total Expenses"
                        font_style: "Body"
                        role: "small"
                        theme_text_color: "Secondary"
                        adaptive_height: True
                        halign: "center"
                
                MDDivider:
                    orientation: "vertical"
                    
                MDBoxLayout:
                    orientation: 'vertical'
                    adaptive_height: True
                    size_hint_x: 0.5
                    
                    MDLabel:
                        text: root.avg_expense
                        font_style: "Headline"
                        role: "small"
                        theme_text_color: "Primary"
                        adaptive_height: True
                        bold: True
                        halign: "center"
                        
                    MDLabel:
                        text: "Avg. Expense"
                        font_style: "Body"
                        role: "small"
                        theme_text_color: "Secondary"
                        adaptive_height: True
                        halign: "center"
            
            # Recent Expenses Header
            MDBoxLayout:
                adaptive_height: True
                padding: [0, dp(8), 0, 0]
                
                MDLabel:
                    text: "Recent Expenses"
                    font_style: "Title"
                    role: "medium"
                    theme_text_color: "Primary"
                    adaptive_height: True
                    bold: True
                    
                MDButton:
                    style: "text"
                    on_release: app.root.ids.screen_manager.current = 'receipts'
                    
                    MDButtonText:
                        text: "See All"
                        theme_text_color: "Custom"
                        text_color: 0, 0.74, 0.83, 1
            
            # Recent Expenses List
            MDBoxLayout:
                id: recent_expenses_container
                orientation: 'vertical'
                adaptive_height: True
                spacing: dp(8)
                
            # Empty State
            MDBoxLayout:
                id: empty_state
                orientation: 'vertical'
                adaptive_height: True
                padding: [0, dp(32), 0, dp(32)]
                spacing: dp(16)
                opacity: 0
                
                MDIcon:
                    icon: "receipt-text-outline"
                    halign: "center"
                    font_size: dp(64)
                    theme_icon_color: "Custom"
                    icon_color: app.theme_cls.onSurfaceVariantColor
                    
                MDLabel:
                    text: "No expenses yet"
                    font_style: "Title"
                    role: "medium"
                    theme_text_color: "Secondary"
                    adaptive_height: True
                    halign: "center"
                    
                MDLabel:
                    text: "Tap + to add your first expense\\nor scan a receipt"
                    font_style: "Body"
                    role: "medium"
                    theme_text_color: "Secondary"
                    adaptive_height: True
                    halign: "center"
                    
                MDButton:
                    style: "filled"
                    pos_hint: {"center_x": 0.5}
                    on_release: app.go_to_add_expense()
                    
                    MDButtonText:
                        text: "Add Expense"
''')


class StatCard(MDCard):
    """Card displaying a statistic with glow effect"""
    title = StringProperty("Title")
    value = StringProperty("€0.00")
    subtitle = StringProperty("")
    icon = StringProperty("chart-line")
    glow_color = ListProperty([0, 0.74, 0.83, 1])


class RecentExpenseItem(MDListItem):
    """List item for recent expenses"""
    expense_id = NumericProperty(0)
    store_name = StringProperty("")
    date_text = StringProperty("")
    amount_text = StringProperty("")
    category_icon = StringProperty("receipt")
    category_color = StringProperty("#00BCD4")
    icon_color = ListProperty([0, 0.74, 0.83, 1])


class DashboardScreen(MDScreen):
    """Main dashboard screen showing spending overview"""
    
    greeting_text = StringProperty("Track your expenses")
    month_total = StringProperty("€0.00")
    month_subtitle = StringProperty("0 expenses")
    week_total = StringProperty("€0.00")
    week_subtitle = StringProperty("0 expenses")
    total_expenses = StringProperty("0")
    avg_expense = StringProperty("€0.00")
    
    def on_enter(self):
        """Called when screen is displayed"""
        Clock.schedule_once(lambda dt: self.refresh_data(), 0.1)
    
    def refresh_data(self):
        """Refresh all dashboard data from database"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        db = app.db
        
        # Update greeting based on time of day
        hour = datetime.now().hour
        if hour < 12:
            self.greeting_text = "Good morning! ☀️"
        elif hour < 18:
            self.greeting_text = "Good afternoon! 🌤️"
        else:
            self.greeting_text = "Good evening! 🌙"
        
        # Get current month data
        now = datetime.now()
        month_start = now.replace(day=1).strftime('%Y-%m-%d')
        month_end = now.strftime('%Y-%m-%d')
        
        month_spending = db.get_total_spending(month_start, month_end)
        month_count = db.get_expense_count(month_start, month_end)
        
        self.month_total = f"€{month_spending:,.2f}"
        self.month_subtitle = f"{month_count} expense{'s' if month_count != 1 else ''}"
        
        # Get last 7 days data
        week_start = (now - timedelta(days=7)).strftime('%Y-%m-%d')
        week_end = now.strftime('%Y-%m-%d')
        
        week_spending = db.get_total_spending(week_start, week_end)
        week_count = db.get_expense_count(week_start, week_end)
        
        self.week_total = f"€{week_spending:,.2f}"
        self.week_subtitle = f"{week_count} expense{'s' if week_count != 1 else ''}"
        
        # Get total stats
        total_count = db.get_expense_count()
        total_spending = db.get_total_spending()
        
        self.total_expenses = str(total_count)
        
        if total_count > 0:
            avg = total_spending / total_count
            self.avg_expense = f"€{avg:,.2f}"
        else:
            self.avg_expense = "€0.00"
        
        # Load recent expenses
        self.load_recent_expenses()
    
    def load_recent_expenses(self):
        """Load recent expenses into the list"""
        from kivymd.app import MDApp
        app = MDApp.get_running_app()
        db = app.db
        
        container = self.ids.recent_expenses_container
        container.clear_widgets()
        
        expenses = db.get_recent_expenses(days=30, limit=5)
        
        # Show/hide empty state
        if not expenses:
            self.ids.empty_state.opacity = 1
            return
        else:
            self.ids.empty_state.opacity = 0
        
        for expense in expenses:
            category_info = db.get_category_info(expense['category'])
            
            # Format date
            try:
                date_obj = datetime.strptime(expense['date'], '%Y-%m-%d')
                date_text = date_obj.strftime('%b %d, %Y')
            except:
                date_text = expense['date']
            
            # Convert hex color to RGBA list
            hex_color = category_info['color']
            rgba = get_color_from_hex(hex_color)
            
            item = RecentExpenseItem(
                expense_id=expense['id'],
                store_name=expense['store_name'],
                date_text=date_text,
                amount_text=f"€{expense['amount']:,.2f}",
                category_icon=category_info['icon'],
                category_color=category_info['color'],
                icon_color=list(rgba)
            )
            container.add_widget(item)
