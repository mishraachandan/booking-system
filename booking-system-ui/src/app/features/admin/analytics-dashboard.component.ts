import { Component, Input, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  AnalyticsService,
  AnalyticsOverview,
  OccupancyEntry,
  RevenuePoint,
  TopShowEntry
} from '../../core/services/analytics.service';

type TabKey = 'overview' | 'revenue' | 'top' | 'occupancy';

// ─── Lightweight inline bar chart ─────────────────────────────────────────────
@Component({
  selector: 'app-bar-chart',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  template: `
    <div class="chart" *ngIf="rows.length; else empty">
      <div class="row" *ngFor="let r of rows">
        <div class="lbl" [title]="r.label">{{ r.label }}</div>
        <div class="track"><div class="fill" [style.width.%]="pct(r.revenue)"></div></div>
        <div class="val">₹{{ r.revenue | number:'1.0-0' }}</div>
      </div>
    </div>
    <ng-template #empty><p class="empty">No data.</p></ng-template>
  `,
  styles: [`
    .chart { display: flex; flex-direction: column; gap: 6px; }
    .row { display: grid; grid-template-columns: minmax(110px, 160px) 1fr 90px; gap: 10px; align-items: center; }
    .lbl { font-size: 12px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .track { height: 12px; background: rgba(255,255,255,0.06); border-radius: 4px; overflow: hidden; }
    .fill { height: 100%; background: linear-gradient(90deg, #6366f1, #22c55e); transition: width 0.3s ease; }
    .val { font-size: 12px; color: var(--text-primary); text-align: right; font-weight: 600; }
    .empty { color: var(--text-muted); font-size: 13px; }
  `]
})
export class BarChartComponent {
  @Input() rows: RevenuePoint[] = [];

  pct(v: number): number {
    const m = Math.max(1, ...(this.rows ?? []).map(r => r.revenue ?? 0));
    return m === 0 ? 0 : Math.max(2, Math.round(((v ?? 0) / m) * 100));
  }
}

@Component({
  selector: 'app-svg-line-chart',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  template: `
    <div class="svg-container" *ngIf="points.length > 0; else empty">
      <svg viewBox="0 0 600 200" preserveAspectRatio="none">
        <defs>
          <linearGradient id="lineGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="#6366f1" stop-opacity="0.3" />
            <stop offset="100%" stop-color="#6366f1" stop-opacity="0" />
          </linearGradient>
        </defs>
        <path [attr.d]="areaPath" fill="url(#lineGrad)" />
        <path [attr.d]="linePath" fill="none" stroke="#6366f1" stroke-width="3" />
        <g *ngFor="let p of points">
          <circle [attr.cx]="p.x" [attr.cy]="p.y" r="5" fill="var(--bg-card)" stroke="#6366f1" stroke-width="2" />
          <title>{{ p.label }}: ₹{{ p.val | number:'1.2-2' }}</title>
        </g>
      </svg>
      <div class="x-axis">
        <span *ngFor="let p of points">{{ p.label }}</span>
      </div>
    </div>
    <ng-template #empty><p class="empty">No data.</p></ng-template>
  `,
  styles: [`
    .svg-container { width: 100%; position: relative; }
    svg { width: 100%; height: 200px; overflow: visible; }
    .x-axis { display: flex; justify-content: space-between; margin-top: 15px; font-size: 11px; color: var(--text-muted); }
    .empty { color: var(--text-muted); font-size: 13px; }
  `]
})
export class SvgLineChartComponent {
  @Input() data: RevenuePoint[] = [];
  
  get points() {
    if (!this.data || this.data.length === 0) return [];
    const maxVal = Math.max(...this.data.map(d => d.revenue), 1);
    const width = 600;
    const height = 180;
    
    return this.data.map((d, i) => {
      const x = this.data.length === 1 ? width / 2 : (i / (this.data.length - 1)) * width;
      const y = height - (d.revenue / maxVal) * height + 10;
      return { x, y, label: (d.label || '').split(' ')[0], val: d.revenue };
    });
  }
  
  get linePath() {
    const pts = this.points;
    if (pts.length === 0) return '';
    return 'M ' + pts.map(p => `${p.x},${p.y}`).join(' L ');
  }
  
  get areaPath() {
    const pts = this.points;
    if (pts.length === 0) return '';
    const start = `M ${pts[0].x},200 L ${pts[0].x},${pts[0].y}`;
    const line = pts.map(p => `L ${p.x},${p.y}`).join(' ');
    const end = `L ${pts[pts.length - 1].x},200 Z`;
    return `${start} ${line} ${end}`;
  }
}

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, DecimalPipe, BarChartComponent, SvgLineChartComponent],
  template: `
    <div class="dash">
      <div class="dash-header">
        <div>
          <h1 class="dash-title">Analytics Dashboard</h1>
          <p class="dash-sub">Occupancy, revenue, top shows and refund rate.</p>
        </div>
        <div class="range-picker">
          <label>From <input type="date" [(ngModel)]="fromDate" (change)="reload()" /></label>
          <label>To <input type="date" [(ngModel)]="toDate" (change)="reload()" /></label>
          <button class="btn-ghost" (click)="reset()">Last 30 days</button>
          <button class="btn-ghost" (click)="exportCsv()" style="background: var(--bg-card);">📥 Export CSV</button>
        </div>
      </div>

      @if (error()) { <div class="alert-error">{{ error() }}</div> }

      <div class="kpi-grid">
        <div class="kpi-card">
          <div class="kpi-label">Total Bookings</div>
          <div class="kpi-value">{{ overview()?.totalBookings ?? '—' }} <span class="trend up">↑</span></div>
          <div class="kpi-sub">
            {{ overview()?.confirmedBookings ?? 0 }} confirmed · {{ overview()?.cancelledBookings ?? 0 }} cancelled
          </div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Total Revenue</div>
          <div class="kpi-value">₹{{ overview()?.totalRevenue | number:'1.0-2' }} <span class="trend up">↑</span></div>
          <div class="kpi-sub">From confirmed bookings</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Average Occupancy</div>
          <div class="kpi-value">{{ getAvgOccupancy() | number:'1.0-1' }}% <span class="trend up">↑</span></div>
          <div class="kpi-sub">Across all shows</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Cancellation Rate</div>
          <div class="kpi-value" [class.warning]="(overview()?.refundRate ?? 0) > 0.1">
            {{ ((overview()?.refundRate ?? 0) * 100) | number:'1.1-2' }}% <span class="trend down">↓</span>
          </div>
          <div class="kpi-sub">Cancelled ÷ total</div>
        </div>
      </div>

      <div class="tabs">
        @for (t of tabs; track t.key) {
          <button class="tab" [class.active]="activeTab() === t.key" (click)="activeTab.set(t.key)">{{ t.label }}</button>
        }
      </div>

      @switch (activeTab()) {
        @case ('revenue') {
          <section class="panel-grid">
            <div class="panel" style="grid-column: 1 / -1;"><h3>Revenue last 7 days</h3><app-svg-line-chart [data]="revenueDaily().slice(-7)"></app-svg-line-chart></div>
            <div class="panel"><h3>Revenue per cinema</h3><app-bar-chart [rows]="revenueByCinema()"></app-bar-chart></div>
            <div class="panel"><h3>Revenue per movie</h3><app-bar-chart [rows]="revenueByMovie()"></app-bar-chart></div>
          </section>
        }
        @case ('top') {
          <section class="panel">
            <h3>Top {{ topShows().length }} performing shows</h3>
            @if (topShows().length === 0) {
              <p class="empty">No shows in this window.</p>
            } @else {
              <table class="tbl">
                <thead><tr>
                  <th>#</th><th>Movie</th><th>Cinema</th><th>Show Time</th>
                  <th>Seats</th><th>Occupancy</th><th>Revenue</th>
                </tr></thead>
                <tbody>
                  @for (s of topShows(); track s.showId; let i = $index) {
                    <tr>
                      <td>{{ i + 1 }}</td>
                      <td>{{ s.movieTitle }}</td>
                      <td>{{ s.cinemaName }}</td>
                      <td>{{ s.startTime | date:'MMM d, h:mm a' }}</td>
                      <td>{{ s.bookedSeats }} / {{ s.totalSeats }}</td>
                      <td>
                        <div class="bar-wrap">
                          <div class="bar" [style.width.%]="s.occupancyPercent"></div>
                          <span>{{ s.occupancyPercent | number:'1.0-1' }}%</span>
                        </div>
                      </td>
                      <td>₹{{ s.revenue | number:'1.0-2' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </section>
        }
        @case ('occupancy') {
          <section class="panel">
            <h3>Occupancy per show</h3>
            @if (occupancy().length === 0) {
              <p class="empty">No shows in this window.</p>
            } @else {
              <table class="tbl">
                <thead><tr>
                  <th>Show Time</th><th>Movie</th><th>Cinema / Screen</th>
                  <th>Booked</th><th>Locked</th><th>Total</th><th>Occupancy</th>
                </tr></thead>
                <tbody>
                  @for (o of occupancy(); track o.showId) {
                    <tr>
                      <td>{{ o.startTime | date:'MMM d, h:mm a' }}</td>
                      <td>{{ o.movieTitle }}</td>
                      <td>{{ o.cinemaName }} · {{ o.screenName }}</td>
                      <td>{{ o.bookedSeats }}</td>
                      <td>{{ o.lockedSeats }}</td>
                      <td>{{ o.totalSeats }}</td>
                      <td>
                        <div class="bar-wrap">
                          <div class="bar" [style.width.%]="o.occupancyPercent"></div>
                          <span>{{ o.occupancyPercent | number:'1.0-1' }}%</span>
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </section>
        }
        @default {
          <section class="panel-grid">
            <div class="panel"><h3>Revenue last 7 days</h3><app-svg-line-chart [data]="revenueDaily().slice(-7)"></app-svg-line-chart></div>
            <div class="panel">
              <h3>Top 5 movies by revenue</h3>
              <table class="tbl">
                <thead><tr>
                  <th>Rank</th><th>Movie</th><th>Total Bookings</th><th>Revenue</th><th>Occupancy %</th>
                </tr></thead>
                <tbody>
                  @for (m of getTopMoviesAggregated(); track m.title; let i = $index) {
                    <tr>
                      <td>
                        @if (i === 0) { <span class="badge gold" style="background: gold; color: #000; padding: 2px 6px; border-radius: 10px; font-weight: bold; font-size: 11px;">#1</span> } @else { #{{ i + 1 }} }
                      </td>
                      <td>{{ m.title }}</td>
                      <td>{{ m.bookings }}</td>
                      <td>₹{{ m.revenue | number:'1.0-2' }}</td>
                      <td>{{ m.occupancy | number:'1.0-1' }}%</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </section>
        }
      }

      @if (loading()) { <div class="loading">Loading…</div> }
    </div>
  `,
  styles: [`
    .dash { max-width: 1280px; }
    .dash-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 24px; margin-bottom: 28px; flex-wrap: wrap; }
    .dash-title { font-family: 'Outfit', sans-serif; font-size: 28px; margin: 0 0 4px; }
    .dash-sub { color: var(--text-muted); margin: 0; font-size: 14px; }
    .range-picker { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
    .range-picker label { font-size: 13px; color: var(--text-muted); display: flex; gap: 6px; align-items: center; }
    .range-picker input { background: var(--bg-card); color: var(--text-primary); border: 1px solid var(--border); padding: 6px 10px; border-radius: 6px; }
    .btn-ghost { background: transparent; border: 1px solid var(--border); color: var(--text-primary); padding: 8px 12px; border-radius: 6px; cursor: pointer; font-size: 13px; }
    .btn-ghost:hover { background: var(--bg-card); }
    .alert-error { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.4); color: #ef4444; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 32px; }
    .kpi-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 18px; }
    .kpi-label { text-transform: uppercase; font-size: 11px; letter-spacing: 1px; color: var(--text-muted); font-weight: 600; }
    .kpi-value { font-family: 'Outfit', sans-serif; font-size: 28px; font-weight: 700; color: var(--text-primary); margin: 6px 0 4px; }
    .kpi-value.warning { color: #f59e0b; }
    .kpi-sub { font-size: 12px; color: var(--text-muted); }
    .tabs { display: flex; gap: 4px; border-bottom: 1px solid var(--border); margin-bottom: 20px; }
    .tab { background: transparent; border: none; color: var(--text-muted); padding: 10px 16px; cursor: pointer; font-size: 14px; border-bottom: 2px solid transparent; transition: var(--transition); font-weight: 500; }
    .tab:hover { color: var(--text-primary); }
    .tab.active { color: var(--accent); border-bottom-color: var(--accent); }
    .panel-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
    @media (max-width: 900px) { .panel-grid { grid-template-columns: 1fr; } }
    .panel { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 20px; }
    .panel h3 { margin: 0 0 14px; font-size: 14px; color: var(--text-primary); font-weight: 600; }
    .empty { color: var(--text-muted); font-size: 13px; padding: 16px 0; }
    .tbl { width: 100%; border-collapse: collapse; font-size: 13px; }
    .tbl th { text-align: left; color: var(--text-muted); font-weight: 600; padding: 10px 8px; border-bottom: 1px solid var(--border); text-transform: uppercase; font-size: 11px; letter-spacing: 0.5px; }
    .tbl td { padding: 10px 8px; border-bottom: 1px solid rgba(255,255,255,0.04); color: var(--text-primary); }
    .bar-wrap { position: relative; height: 18px; background: rgba(255,255,255,0.06); border-radius: 4px; overflow: hidden; min-width: 120px; }
    .bar { position: absolute; top: 0; left: 0; bottom: 0; background: linear-gradient(90deg, #6366f1, #22c55e); }
    .bar-wrap span { position: relative; z-index: 1; font-size: 11px; line-height: 18px; padding-left: 8px; color: var(--text-primary); font-weight: 600; }
    .loading { margin-top: 16px; color: var(--text-muted); font-size: 13px; text-align: center; }
    .trend { font-size: 14px; font-weight: normal; margin-left: 4px; }
    .trend.up { color: #22c55e; }
    .trend.down { color: #ef4444; }
  `]
})
export class AnalyticsDashboardComponent implements OnInit {
  private analytics = inject(AnalyticsService);

  readonly tabs: { key: TabKey; label: string }[] = [
    { key: 'overview',  label: 'Overview' },
    { key: 'revenue',   label: 'Revenue' },
    { key: 'top',       label: 'Top Shows' },
    { key: 'occupancy', label: 'Occupancy' }
  ];

  activeTab = signal<TabKey>('overview');
  loading = signal(false);
  error = signal<string | null>(null);

  overview = signal<AnalyticsOverview | null>(null);
  revenueDaily = signal<RevenuePoint[]>([]);
  revenueByCinema = signal<RevenuePoint[]>([]);
  revenueByMovie = signal<RevenuePoint[]>([]);
  topShows = signal<TopShowEntry[]>([]);
  occupancy = signal<OccupancyEntry[]>([]);

  fromDate = '';
  toDate = '';

  ngOnInit() { this.reset(); }

  reset() {
    const today = new Date();
    const from = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000);
    this.fromDate = from.toISOString().slice(0, 10);
    this.toDate = today.toISOString().slice(0, 10);
    this.reload();
  }

  getAvgOccupancy(): number {
    const occ = this.occupancy();
    if (!occ || occ.length === 0) return 0;
    const totalBooked = occ.reduce((sum, o) => sum + (o.bookedSeats || 0), 0);
    const totalSeats = occ.reduce((sum, o) => sum + (o.totalSeats || 0), 0);
    return totalSeats > 0 ? (totalBooked / totalSeats) * 100 : 0;
  }

  getTopMoviesAggregated(): { title: string, bookings: number, revenue: number, occupancy: number }[] {
    const shows = this.topShows();
    if (!shows || shows.length === 0) return [];
    
    const movieMap = new Map<string, { bookings: number, revenue: number, totalSeats: number, bookedSeats: number }>();
    for (const s of shows) {
      if (!s.movieTitle) continue;
      if (!movieMap.has(s.movieTitle)) {
        movieMap.set(s.movieTitle, { bookings: 0, revenue: 0, totalSeats: 0, bookedSeats: 0 });
      }
      const m = movieMap.get(s.movieTitle)!;
      m.bookings += 1;
      m.revenue += (s.revenue || 0);
      m.totalSeats += (s.totalSeats || 0);
      m.bookedSeats += (s.bookedSeats || 0);
    }
    
    return Array.from(movieMap.entries()).map(([title, stats]) => ({
      title,
      bookings: stats.bookings,
      revenue: stats.revenue,
      occupancy: stats.totalSeats > 0 ? (stats.bookedSeats / stats.totalSeats) * 100 : 0
    })).sort((a, b) => b.revenue - a.revenue).slice(0, 5);
  }

  exportCsv() {
    const url = `/api/v1/admin/export/bookings?from=${this.fromDate}&to=${this.toDate}&format=csv`;
    window.open(url, '_blank');
  }

  reload() {
    this.loading.set(true);
    this.error.set(null);
    const range = { from: this.fromDate, to: this.toDate };
    forkJoin({
      overview: this.analytics.overview(range),
      daily: this.analytics.revenueDaily(range),
      byCinema: this.analytics.revenueByCinema(range),
      byMovie: this.analytics.revenueByMovie(range),
      topShows: this.analytics.topShows(range, 10),
      occupancy: this.analytics.occupancy()
    }).subscribe({
      next: r => {
        this.overview.set(r.overview);
        this.revenueDaily.set(r.daily);
        this.revenueByCinema.set(r.byCinema);
        this.revenueByMovie.set(r.byMovie);
        this.topShows.set(r.topShows);
        this.occupancy.set(r.occupancy);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        if (err?.status === 403) {
          this.error.set('Access denied. Admin role required.');
        } else {
          this.error.set(err?.error?.message ?? err?.message ?? 'Failed to load analytics.');
        }
      }
    });
  }
}
