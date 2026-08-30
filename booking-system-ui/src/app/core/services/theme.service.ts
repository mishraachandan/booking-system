import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private currentTheme = signal<'dark' | 'light'>('dark');

  theme = this.currentTheme.asReadonly();

  constructor() {
    // Check if we are running in a browser environment before using localStorage/document
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('theme') as 'dark' | 'light' | null;
      if (saved) {
        this.setTheme(saved);
      } else {
        this.setTheme('dark');
      }
    }
  }

  toggleTheme() {
    this.setTheme(this.currentTheme() === 'dark' ? 'light' : 'dark');
  }

  setTheme(theme: 'dark' | 'light') {
    this.currentTheme.set(theme);
    if (typeof window !== 'undefined') {
      localStorage.setItem('theme', theme);
      if (theme === 'light') {
        document.body.classList.add('light-theme');
      } else {
        document.body.classList.remove('light-theme');
      }
    }
  }
}
