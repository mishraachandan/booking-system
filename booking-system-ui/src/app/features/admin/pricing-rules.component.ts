import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PricingRule, PricingRuleService } from '../../core/services/pricing-rule.service';

@Component({
  selector: 'app-pricing-rules',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Dynamic Pricing Rules</h1>
          <p class="page-sub">
            Rules are applied multiplicatively in priority order. With no rules (or when the feature flag is off),
            seats are priced at their base amount — the booking flow is identical to pre-dynamic-pricing behaviour.
          </p>
        </div>
        <div class="flag-pill" [class.on]="enabled()">
          Feature flag: {{ enabled() ? 'ON' : 'OFF (dry run)' }}
        </div>
      </div>

      @if (error()) { <div class="alert-error">{{ error() }}</div> }
      @if (success()) { <div class="alert-ok">{{ success() }}</div> }

      <section class="panel">
        <div class="panel-head">
          <h3>Existing Rules ({{ rules().length }})</h3>
          <button class="btn-primary" (click)="startCreate()">+ New Rule</button>
        </div>

        @if (rules().length === 0) {
          <p class="empty">No rules defined. Seats are priced at base.</p>
        } @else {
          <table class="tbl">
            <thead>
              <tr>
                <th>Priority</th><th>Name</th><th>Active</th><th>Days</th>
                <th>Hours</th><th>Lead Time</th><th>Multiplier</th><th></th>
              </tr>
            </thead>
            <tbody>
              @for (r of rules(); track r.id) {
                <tr>
                  <td>{{ r.priority }}</td>
                  <td>
                    <div class="nm">{{ r.name }}</div>
                    <div class="dsc" *ngIf="r.description">{{ r.description }}</div>
                  </td>
                  <td>
                    <span class="dot" [class.on]="r.active"></span>
                    {{ r.active ? 'Yes' : 'No' }}
                  </td>
                  <td>{{ r.daysOfWeek || 'any' }}</td>
                  <td>{{ formatHours(r) }}</td>
                  <td>{{ formatLead(r) }}</td>
                  <td class="mul" [class.up]="r.multiplier > 1" [class.down]="r.multiplier < 1">
                    ×{{ r.multiplier | number:'1.2-3' }}
                  </td>
                  <td class="actions">
                    <button class="btn-link" (click)="startEdit(r)">Edit</button>
                    <button class="btn-link danger" (click)="remove(r)">Delete</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </section>

      <!-- Preset seed button -->
      @if (rules().length === 0) {
        <div class="seed-hint">
          <strong>Suggested starter rules:</strong> Weekend surge (×1.20), Evening peak (×1.15), Early-bird discount (×0.80 if >48h out).
          <button class="btn-ghost" (click)="seedDefaults()">Seed suggested rules</button>
        </div>
      }

      <!-- Create/Edit Modal -->
      @if (editing()) {
        <div class="modal-backdrop" (click)="cancel()"></div>
        <div class="modal">
          <div class="modal-head">
            <h3>{{ editing()!.id ? 'Edit Rule' : 'New Rule' }}</h3>
            <button class="close" (click)="cancel()">×</button>
          </div>
          <form class="form" (ngSubmit)="save()">
            <label>Name <input type="text" name="name" [(ngModel)]="editing()!.name" required maxlength="128" /></label>
            <label>Description <input type="text" name="description" [(ngModel)]="editing()!.description" maxlength="512" /></label>
            <div class="row">
              <label class="chk">
                <input type="checkbox" name="active" [(ngModel)]="editing()!.active" />
                Active
              </label>
              <label>Priority
                <input type="number" name="priority" [(ngModel)]="editing()!.priority" min="0" max="10000" />
              </label>
            </div>
            <label>Days of week (CSV, e.g. SAT,SUN — leave blank for any)
              <input type="text" name="daysOfWeek" [(ngModel)]="editing()!.daysOfWeek" placeholder="SAT,SUN" />
            </label>
            <div class="row">
              <label>Start hour (0-23)
                <input type="number" name="startHour" [(ngModel)]="editing()!.startHour" min="0" max="23" />
              </label>
              <label>End hour (0-23)
                <input type="number" name="endHour" [(ngModel)]="editing()!.endHour" min="0" max="23" />
              </label>
            </div>
            <div class="row">
              <label>Min lead time (hours)
                <input type="number" name="minLeadTimeHours" [(ngModel)]="editing()!.minLeadTimeHours" min="0" max="10000" />
              </label>
              <label>Max lead time (hours)
                <input type="number" name="maxLeadTimeHours" [(ngModel)]="editing()!.maxLeadTimeHours" min="0" max="10000" />
              </label>
            </div>
            <label>Multiplier (0.10 – 5.00)
              <input type="number" name="multiplier" [(ngModel)]="editing()!.multiplier" min="0.1" max="5" step="0.01" required />
            </label>
            <div class="form-actions">
              <button type="button" class="btn-ghost" (click)="cancel()">Cancel</button>
              <button type="submit" class="btn-primary" [disabled]="saving()">
                {{ saving() ? 'Saving…' : (editing()!.id ? 'Update' : 'Create') }}
              </button>
            </div>
          </form>
        </div>
      }
    </div>
  `,
  styles: [`
    .page { max-width: 1100px; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; margin-bottom: 24px; flex-wrap: wrap; }
    .page-title { font-family: 'Outfit', sans-serif; font-size: 28px; margin: 0 0 6px; }
    .page-sub { color: var(--text-muted); margin: 0; max-width: 760px; font-size: 13px; line-height: 1.5; }
    .flag-pill { font-size: 12px; font-weight: 600; padding: 6px 12px; border-radius: 999px; background: rgba(239,68,68,0.15); color: #fca5a5; border: 1px solid rgba(239,68,68,0.4); }
    .flag-pill.on { background: rgba(34,197,94,0.15); color: #86efac; border-color: rgba(34,197,94,0.4); }
    .alert-error { background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.4); color: #ef4444; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .alert-ok { background: rgba(34,197,94,0.1); border: 1px solid rgba(34,197,94,0.4); color: #22c55e; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .panel { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 20px; }
    .panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
    .panel-head h3 { margin: 0; font-size: 14px; }
    .empty { color: var(--text-muted); font-size: 13px; }
    .tbl { width: 100%; border-collapse: collapse; font-size: 13px; }
    .tbl th { text-align: left; color: var(--text-muted); font-weight: 600; padding: 10px 8px; border-bottom: 1px solid var(--border); text-transform: uppercase; font-size: 11px; letter-spacing: 0.5px; }
    .tbl td { padding: 10px 8px; border-bottom: 1px solid rgba(255,255,255,0.04); color: var(--text-primary); vertical-align: top; }
    .tbl .nm { font-weight: 600; }
    .tbl .dsc { color: var(--text-muted); font-size: 12px; margin-top: 2px; }
    .dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #6b7280; margin-right: 6px; }
    .dot.on { background: #22c55e; }
    .mul { font-family: 'JetBrains Mono', monospace; font-weight: 600; }
    .mul.up { color: #f59e0b; }
    .mul.down { color: #22c55e; }
    .actions { white-space: nowrap; }
    .btn-link { background: transparent; border: none; color: var(--accent); cursor: pointer; margin-right: 8px; font-size: 12px; padding: 0; }
    .btn-link.danger { color: #ef4444; }
    .btn-link:hover { text-decoration: underline; }
    .btn-primary { background: var(--accent); color: #fff; border: none; padding: 8px 14px; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 13px; }
    .btn-primary:hover { background: var(--accent-hover); }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-ghost { background: transparent; border: 1px solid var(--border); color: var(--text-primary); padding: 8px 14px; border-radius: 6px; cursor: pointer; font-size: 13px; }
    .btn-ghost:hover { background: var(--bg-card); }
    .seed-hint { margin-top: 16px; font-size: 13px; color: var(--text-muted); padding: 12px 16px; background: rgba(99,102,241,0.08); border: 1px solid rgba(99,102,241,0.3); border-radius: 8px; display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
    .modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,0.65); z-index: 50; }
    .modal { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 24px; width: min(92vw, 520px); z-index: 51; max-height: 90vh; overflow-y: auto; }
    .modal-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .modal-head h3 { margin: 0; font-size: 16px; }
    .close { background: transparent; border: none; color: var(--text-muted); font-size: 22px; cursor: pointer; line-height: 1; }
    .form { display: flex; flex-direction: column; gap: 12px; }
    .form label { font-size: 12px; color: var(--text-muted); display: flex; flex-direction: column; gap: 4px; }
    .form label.chk { flex-direction: row; align-items: center; gap: 8px; color: var(--text-primary); font-size: 13px; }
    .form input[type=text], .form input[type=number] { background: var(--bg-main); color: var(--text-primary); border: 1px solid var(--border); padding: 8px 10px; border-radius: 6px; font-size: 13px; }
    .form .row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .form-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 8px; }
  `]
})
export class PricingRulesComponent implements OnInit {
  private svc = inject(PricingRuleService);

  rules = signal<PricingRule[]>([]);
  enabled = signal(false);
  editing = signal<PricingRule | null>(null);
  saving = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);

  ngOnInit() {
    this.reload();
    this.svc.status().subscribe({
      next: s => this.enabled.set(s.enabled),
      error: () => this.enabled.set(false)
    });
  }

  reload() {
    this.svc.list().subscribe({
      next: r => this.rules.set(r ?? []),
      error: err => this.error.set(err?.error?.message ?? 'Failed to load rules.')
    });
  }

  startCreate() {
    this.editing.set({
      name: '',
      description: '',
      active: true,
      priority: 100,
      daysOfWeek: null,
      startHour: null,
      endHour: null,
      minLeadTimeHours: null,
      maxLeadTimeHours: null,
      multiplier: 1.0
    });
  }

  startEdit(r: PricingRule) {
    this.editing.set({ ...r });
  }

  cancel() { this.editing.set(null); }

  save() {
    const r = this.editing();
    if (!r) return;
    this.saving.set(true);
    this.error.set(null);
    const req = r.id ? this.svc.update(r.id, r) : this.svc.create(r);
    req.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(null);
        this.success.set(r.id ? 'Rule updated.' : 'Rule created.');
        setTimeout(() => this.success.set(null), 3000);
        this.reload();
      },
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? err?.error ?? 'Save failed.');
      }
    });
  }

  remove(r: PricingRule) {
    if (!r.id) return;
    if (!confirm(`Delete rule "${r.name}"?`)) return;
    this.svc.delete(r.id).subscribe({
      next: () => {
        this.success.set('Rule deleted.');
        setTimeout(() => this.success.set(null), 3000);
        this.reload();
      },
      error: err => this.error.set(err?.error?.message ?? 'Delete failed.')
    });
  }

  seedDefaults() {
    const defaults: PricingRule[] = [
      { name: 'Weekend surge',        description: 'Sat & Sun shows', active: true, priority: 10,
        daysOfWeek: 'SAT,SUN', startHour: null, endHour: null,
        minLeadTimeHours: null, maxLeadTimeHours: null, multiplier: 1.20 },
      { name: 'Evening peak',         description: '6 PM – 10 PM shows', active: true, priority: 20,
        daysOfWeek: null, startHour: 18, endHour: 22,
        minLeadTimeHours: null, maxLeadTimeHours: null, multiplier: 1.15 },
      { name: 'Early-bird discount',  description: 'Booked > 48h in advance', active: true, priority: 30,
        daysOfWeek: null, startHour: null, endHour: null,
        minLeadTimeHours: 48, maxLeadTimeHours: null, multiplier: 0.80 }
    ];
    let remaining = defaults.length;
    defaults.forEach(d => {
      this.svc.create(d).subscribe({
        next: () => { if (--remaining === 0) { this.success.set('Seeded 3 suggested rules.'); this.reload(); } },
        error: () => { if (--remaining === 0) { this.reload(); } }
      });
    });
  }

  formatHours(r: PricingRule): string {
    if (r.startHour == null && r.endHour == null) return 'any';
    const s = r.startHour == null ? '00' : String(r.startHour).padStart(2, '0');
    const e = r.endHour == null ? '23' : String(r.endHour).padStart(2, '0');
    return `${s}:00–${e}:59`;
  }

  formatLead(r: PricingRule): string {
    if (r.minLeadTimeHours == null && r.maxLeadTimeHours == null) return 'any';
    const parts: string[] = [];
    if (r.minLeadTimeHours != null) parts.push(`≥${r.minLeadTimeHours}h`);
    if (r.maxLeadTimeHours != null) parts.push(`≤${r.maxLeadTimeHours}h`);
    return parts.join(' ');
  }
}
