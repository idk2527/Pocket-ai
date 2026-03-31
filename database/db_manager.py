"""
Database Manager for Pocket AI
Handles all SQLite operations for expenses and receipts
"""

import sqlite3
import os
from datetime import datetime, timedelta
from kivy.utils import platform

if platform == 'android':
    from android.storage import app_storage_path
    DB_PATH = os.path.join(app_storage_path(), 'pocket_ai.db')
else:
    DB_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'pocket_ai.db')


class DatabaseManager:
    """Manages SQLite database operations for Pocket AI"""
    
    CATEGORIES = [
        ('groceries', 'Groceries', 'cart'),
        ('electronics', 'Electronics', 'laptop'),
        ('health_beauty', 'Health & Beauty', 'heart-pulse'),
        ('fashion', 'Fashion', 'tshirt-crew'),
        ('home', 'Home', 'home'),
        ('food_dining', 'Food & Dining', 'food'),
        ('transport', 'Transport', 'car'),
        ('entertainment', 'Entertainment', 'movie'),
        ('other', 'Other', 'dots-horizontal'),
    ]
    
    CATEGORY_COLORS = {
        'groceries': '#4CAF50',
        'electronics': '#2196F3',
        'health_beauty': '#E91E63',
        'fashion': '#9C27B0',
        'home': '#FF9800',
        'food_dining': '#F44336',
        'transport': '#00BCD4',
        'entertainment': '#FFEB3B',
        'other': '#607D8B',
    }
    
    def __init__(self):
        self.db_path = DB_PATH
        
    def get_connection(self):
        """Get database connection"""
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn
    
    def init_db(self):
        """Initialize database with required tables"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        # Create expenses table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                store_name TEXT NOT NULL,
                category TEXT NOT NULL,
                amount REAL NOT NULL,
                currency TEXT DEFAULT 'EUR',
                date TEXT NOT NULL,
                note TEXT,
                receipt_image TEXT,
                items TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        # Create index for faster queries
        cursor.execute('''
            CREATE INDEX IF NOT EXISTS idx_expenses_date ON expenses(date)
        ''')
        cursor.execute('''
            CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(category)
        ''')
        
        conn.commit()
        conn.close()
        
    def add_expense(self, store_name, category, amount, date, currency='EUR', 
                    note=None, receipt_image=None, items=None):
        """Add a new expense to the database"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        cursor.execute('''
            INSERT INTO expenses (store_name, category, amount, currency, date, note, receipt_image, items)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ''', (store_name, category, amount, currency, date, note, receipt_image, items))
        
        expense_id = cursor.lastrowid
        conn.commit()
        conn.close()
        
        return expense_id
    
    def update_expense(self, expense_id, **kwargs):
        """Update an existing expense"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        # Build update query dynamically
        fields = []
        values = []
        for key, value in kwargs.items():
            if key in ['store_name', 'category', 'amount', 'currency', 'date', 'note', 'receipt_image', 'items']:
                fields.append(f"{key} = ?")
                values.append(value)
        
        if fields:
            fields.append("updated_at = ?")
            values.append(datetime.now().isoformat())
            values.append(expense_id)
            
            query = f"UPDATE expenses SET {', '.join(fields)} WHERE id = ?"
            cursor.execute(query, values)
            
        conn.commit()
        conn.close()
    
    def delete_expense(self, expense_id):
        """Delete an expense"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        # Get receipt image path before deleting
        cursor.execute('SELECT receipt_image FROM expenses WHERE id = ?', (expense_id,))
        row = cursor.fetchone()
        
        if row and row['receipt_image']:
            # Delete the receipt image file if it exists
            if os.path.exists(row['receipt_image']):
                try:
                    os.remove(row['receipt_image'])
                except:
                    pass
        
        cursor.execute('DELETE FROM expenses WHERE id = ?', (expense_id,))
        conn.commit()
        conn.close()
    
    def get_expense(self, expense_id):
        """Get a single expense by ID"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        cursor.execute('SELECT * FROM expenses WHERE id = ?', (expense_id,))
        row = cursor.fetchone()
        
        conn.close()
        
        if row:
            return dict(row)
        return None
    
    def get_all_expenses(self, limit=None, offset=0):
        """Get all expenses ordered by date descending"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        query = 'SELECT * FROM expenses ORDER BY date DESC, created_at DESC'
        if limit:
            query += f' LIMIT {limit} OFFSET {offset}'
            
        cursor.execute(query)
        rows = cursor.fetchall()
        
        conn.close()
        
        return [dict(row) for row in rows]
    
    def get_expenses_by_date_range(self, start_date, end_date):
        """Get expenses within a date range"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT * FROM expenses 
            WHERE date >= ? AND date <= ?
            ORDER BY date DESC, created_at DESC
        ''', (start_date, end_date))
        
        rows = cursor.fetchall()
        conn.close()
        
        return [dict(row) for row in rows]
    
    def get_expenses_by_category(self, category):
        """Get expenses by category"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT * FROM expenses 
            WHERE category = ?
            ORDER BY date DESC, created_at DESC
        ''', (category,))
        
        rows = cursor.fetchall()
        conn.close()
        
        return [dict(row) for row in rows]
    
    def get_total_spending(self, start_date=None, end_date=None):
        """Get total spending, optionally within a date range"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        if start_date and end_date:
            cursor.execute('''
                SELECT COALESCE(SUM(amount), 0) as total 
                FROM expenses 
                WHERE date >= ? AND date <= ?
            ''', (start_date, end_date))
        else:
            cursor.execute('SELECT COALESCE(SUM(amount), 0) as total FROM expenses')
        
        row = cursor.fetchone()
        conn.close()
        
        return row['total'] if row else 0
    
    def get_expense_count(self, start_date=None, end_date=None):
        """Get count of expenses, optionally within a date range"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        if start_date and end_date:
            cursor.execute('''
                SELECT COUNT(*) as count 
                FROM expenses 
                WHERE date >= ? AND date <= ?
            ''', (start_date, end_date))
        else:
            cursor.execute('SELECT COUNT(*) as count FROM expenses')
        
        row = cursor.fetchone()
        conn.close()
        
        return row['count'] if row else 0
    
    def get_category_totals(self, start_date=None, end_date=None):
        """Get spending totals by category"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        if start_date and end_date:
            cursor.execute('''
                SELECT category, SUM(amount) as total, COUNT(*) as count
                FROM expenses 
                WHERE date >= ? AND date <= ?
                GROUP BY category
                ORDER BY total DESC
            ''', (start_date, end_date))
        else:
            cursor.execute('''
                SELECT category, SUM(amount) as total, COUNT(*) as count
                FROM expenses 
                GROUP BY category
                ORDER BY total DESC
            ''')
        
        rows = cursor.fetchall()
        conn.close()
        
        return [dict(row) for row in rows]
    
    def get_recent_expenses(self, days=7, limit=10):
        """Get recent expenses from the last N days"""
        end_date = datetime.now().strftime('%Y-%m-%d')
        start_date = (datetime.now() - timedelta(days=days)).strftime('%Y-%m-%d')
        
        conn = self.get_connection()
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT * FROM expenses 
            WHERE date >= ? AND date <= ?
            ORDER BY date DESC, created_at DESC
            LIMIT ?
        ''', (start_date, end_date, limit))
        
        rows = cursor.fetchall()
        conn.close()
        
        return [dict(row) for row in rows]
    
    def get_monthly_spending(self, year=None, month=None):
        """Get spending for a specific month"""
        if year is None:
            year = datetime.now().year
        if month is None:
            month = datetime.now().month
            
        start_date = f"{year}-{month:02d}-01"
        
        # Calculate end of month
        if month == 12:
            end_date = f"{year + 1}-01-01"
        else:
            end_date = f"{year}-{month + 1:02d}-01"
        
        conn = self.get_connection()
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT COALESCE(SUM(amount), 0) as total 
            FROM expenses 
            WHERE date >= ? AND date < ?
        ''', (start_date, end_date))
        
        row = cursor.fetchone()
        conn.close()
        
        return row['total'] if row else 0
    
    def get_daily_spending(self, days=30):
        """Get daily spending for the last N days"""
        end_date = datetime.now()
        start_date = end_date - timedelta(days=days)
        
        conn = self.get_connection()
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT date, SUM(amount) as total
            FROM expenses 
            WHERE date >= ? AND date <= ?
            GROUP BY date
            ORDER BY date ASC
        ''', (start_date.strftime('%Y-%m-%d'), end_date.strftime('%Y-%m-%d')))
        
        rows = cursor.fetchall()
        conn.close()
        
        return [dict(row) for row in rows]
    
    def search_expenses(self, query):
        """Search expenses by store name or note"""
        conn = self.get_connection()
        cursor = conn.cursor()
        
        search_term = f"%{query}%"
        cursor.execute('''
            SELECT * FROM expenses 
            WHERE store_name LIKE ? OR note LIKE ?
            ORDER BY date DESC, created_at DESC
        ''', (search_term, search_term))
        
        rows = cursor.fetchall()
        conn.close()
        
        return [dict(row) for row in rows]
    
    def get_category_info(self, category_id):
        """Get category display name and icon"""
        for cat_id, name, icon in self.CATEGORIES:
            if cat_id == category_id:
                return {
                    'id': cat_id,
                    'name': name,
                    'icon': icon,
                    'color': self.CATEGORY_COLORS.get(cat_id, '#607D8B')
                }
        return {
            'id': 'other',
            'name': 'Other',
            'icon': 'dots-horizontal',
            'color': '#607D8B'
        }
