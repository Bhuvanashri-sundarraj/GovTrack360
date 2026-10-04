import React, { useCallback, useEffect, useState } from 'react'
import { Download, Printer, Play } from 'lucide-react'
import { useToast } from '../components/ToastContext'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import DateRangeFilter from '../components/DateRangeFilter'
import { getReportTypes, generateReport, exportReportCsv } from '../services/reportService'
import { getPerformanceScope } from '../services/performanceService'
import { getDepartments } from '../services/departmentService'
import { rangeFromDays, fmtNum, formatDate, apiError, blobError, downloadBlob } from '../utils/performance'

function formatCell(column, value) {
  if (value == null || value === '') return '—'
  if (column.type === 'number') return fmtNum(value)
  return String(value)
}

export default function Reports() {
  const toast = useToast()
  const [types, setTypes] = useState([])
  const [scope, setScope] = useState(null)
  const [departments, setDepartments] = useState([])

  const [type, setType] = useState('TASK_SUMMARY')
  const [departmentId, setDepartmentId] = useState('')
  const [range, setRange] = useState(rangeFromDays(90))

  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [exporting, setExporting] = useState(false)

  useEffect(() => {
    const loadMeta = async () => {
      try {
        const [t, s] = await Promise.all([getReportTypes(), getPerformanceScope()])
        setTypes(t)
        setScope(s)
        if (!s.restricted) setDepartments(await getDepartments())
      } catch (err) {
        setError('Could not load report options.')
      }
    }
    loadMeta()
  }, [])

  const params = useCallback(() => ({
    departmentId: departmentId || undefined,
    from: range.from,
    to: range.to,
  }), [departmentId, range])

  const run = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setReport(await generateReport(type, params()))
    } catch (err) {
      setReport(null)
      setError(apiError(err, 'Could not generate the report.'))
    } finally {
      setLoading(false)
    }
  }, [type, params])

  // Run once on load so the page is never blank; after that the user clicks "Generate".
  useEffect(() => { run() }, []) // eslint-disable-line

  const handleExport = async () => {
    setExporting(true)
    try {
      const blob = await exportReportCsv(type, params())
      downloadBlob(blob, `govtrack360-${type.toLowerCase().replace(/_/g, '-')}-${range.from}-to-${range.to}.csv`)
      toast.success('Report exported.')
    } catch (err) {
      toast.error(await blobError(err, 'Failed to export the report.'))
    } finally {
      setExporting(false)
    }
  }

  const selected = types.find((t) => t.value === type)

  return (
    <div>
      <div className="mb-6 no-print">
        <h1 className="text-xl font-semibold text-slate-800">Reports</h1>
        <p className="text-sm text-slate-500 mt-1">
          {scope?.restricted && scope.departmentName
            ? `Reports for the ${scope.departmentName} department.`
            : 'Generate, print or export reports for any period.'}
        </p>
      </div>

      <div className="card p-4 mb-6 no-print">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
          <div>
            <label className="label" htmlFor="report-type">Report</label>
            <select id="report-type" className="input" value={type} onChange={(e) => setType(e.target.value)}>
              {types.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
          </div>
          {!scope?.restricted && (
            <div>
              <label className="label" htmlFor="report-dept">Department</label>
              <select id="report-dept" className="input" value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
                <option value="">All Departments</option>
                {departments.map((d) => <option key={d.id} value={d.id}>{d.departmentName}</option>)}
              </select>
            </div>
          )}
          <div>
            <label className="label">Period</label>
            <DateRangeFilter value={range} onChange={setRange} />
          </div>
        </div>
        {selected && <p className="text-xs text-slate-400 mt-3">{selected.description}</p>}
        <div className="flex gap-3 mt-4">
          <button className="btn-primary" onClick={run} disabled={loading}><Play className="w-4 h-4" /> {loading ? 'Generating...' : 'Generate'}</button>
          <button className="btn-secondary" onClick={handleExport} disabled={exporting || loading}><Download className="w-4 h-4" /> {exporting ? 'Exporting...' : 'Export CSV'}</button>
          <button className="btn-secondary" onClick={() => window.print()} disabled={!report}><Printer className="w-4 h-4" /> Print / Save PDF</button>
        </div>
      </div>

      {loading ? (
        <Spinner label="Generating report..." />
      ) : error ? (
        <ErrorState message={error} onRetry={run} />
      ) : report ? (
        <div className="card overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-200">
            <h2 className="text-base font-semibold text-slate-800">{report.title}</h2>
            <p className="text-xs text-slate-500 mt-1">
              {report.scope} · {formatDate(report.from)} – {formatDate(report.to)} · generated {new Date(report.generatedAt).toLocaleString()} by {report.generatedBy}
            </p>
          </div>

          {report.summary.length > 0 && (
            <div className="px-5 py-4 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 border-b border-slate-100">
              {report.summary.map((item) => (
                <div key={item.label} className="rounded-md bg-slate-50 p-3">
                  <p className="text-xs text-slate-500">{item.label}</p>
                  <p className="text-sm font-semibold text-slate-800 mt-0.5 break-words">{item.value}</p>
                </div>
              ))}
            </div>
          )}

          {report.rows.length === 0 ? (
            <EmptyState title="No data for this report" description="Try a wider period or a different department." />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 border-b border-slate-200">
                  <tr>
                    {report.columns.map((c) => (
                      <th key={c.key} className={`px-4 py-3 font-medium text-slate-500 whitespace-nowrap ${c.type === 'number' ? 'text-right' : 'text-left'}`}>{c.label}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {report.rows.map((row, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      {report.columns.map((c) => (
                        <td key={c.key} className={`px-4 py-2.5 text-slate-700 ${c.type === 'number' ? 'text-right tabular-nums' : 'text-left'}`}>
                          {formatCell(c, row[c.key])}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <p className="px-5 py-3 text-xs text-slate-400 border-t border-slate-100">{report.rows.length} row(s)</p>
        </div>
      ) : null}
    </div>
  )
}
