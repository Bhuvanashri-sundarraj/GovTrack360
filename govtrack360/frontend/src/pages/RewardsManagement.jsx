import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Plus, Trash2, Award, Star, Users } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/ToastContext'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import StatCard from '../components/StatCard'
import Tabs from '../components/Tabs'
import ConfirmDialog from '../components/ConfirmDialog'
import GrantRewardModal from '../components/GrantRewardModal'
import RewardTypeBadge from '../components/RewardTypeBadge'
import { getRewards, getRewardLeaderboard, getRewardTypes, revokeReward } from '../services/rewardService'
import { getPerformanceScope } from '../services/performanceService'
import { getDepartments } from '../services/departmentService'
import { formatDate, apiError } from '../utils/performance'

export default function RewardsManagement() {
  const { user } = useAuth()
  const toast = useToast()
  const isAdmin = user?.role === 'ADMIN'

  const [tab, setTab] = useState('recognitions')
  const [scope, setScope] = useState(null)
  const [departments, setDepartments] = useState([])
  const [types, setTypes] = useState([])

  const [departmentId, setDepartmentId] = useState('')
  const [type, setType] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  const [rewards, setRewards] = useState([])
  const [board, setBoard] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const [grantOpen, setGrantOpen] = useState(false)
  const [revokeTarget, setRevokeTarget] = useState(null)
  const [revoking, setRevoking] = useState(false)

  useEffect(() => {
    const loadMeta = async () => {
      try {
        const [s, t] = await Promise.all([getPerformanceScope(), getRewardTypes()])
        setScope(s)
        setTypes(t)
        if (!s.restricted) setDepartments(await getDepartments())
      } catch (err) {
        setScope({ restricted: true })
      }
    }
    loadMeta()
  }, [])

  const dateInvalid = !!from && !!to && from > to

  const load = useCallback(async () => {
    if (dateInvalid) return
    setLoading(true)
    setError(false)
    try {
      const base = { departmentId: departmentId || undefined, from: from || undefined, to: to || undefined }
      const [list, lb] = await Promise.all([
        getRewards({ ...base, type: type || undefined }),
        getRewardLeaderboard(base),
      ])
      setRewards(list)
      setBoard(lb)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [departmentId, type, from, to, dateInvalid])

  useEffect(() => { load() }, [load])

  const totals = useMemo(() => ({
    count: rewards.length,
    points: rewards.reduce((s, r) => s + r.points, 0),
    people: new Set(rewards.map((r) => r.employee.id)).size,
  }), [rewards])

  const canRevoke = (r) => isAdmin || r.awardedBy?.id === user?.id

  const handleRevoke = async () => {
    setRevoking(true)
    try {
      await revokeReward(revokeTarget.id)
      toast.success('Reward revoked.')
      setRevokeTarget(null)
      load()
    } catch (err) {
      toast.error(apiError(err, 'Failed to revoke reward.'))
    } finally {
      setRevoking(false)
    }
  }

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">Rewards &amp; Recognition</h1>
          <p className="text-sm text-slate-500 mt-1">
            {scope?.restricted && scope.departmentName
              ? `Recognise the ${scope.departmentName} team.`
              : 'Recognise outstanding work across the organisation.'}
          </p>
        </div>
        <button className="btn-primary self-start" onClick={() => setGrantOpen(true)}>
          <Plus className="w-4 h-4" /> Grant reward
        </button>
      </div>

      <div className="card p-4 mb-4 flex flex-col lg:flex-row gap-3">
        {!scope?.restricted && (
          <select className="input lg:w-52" value={departmentId} onChange={(e) => setDepartmentId(e.target.value)} aria-label="Department">
            <option value="">All Departments</option>
            {departments.map((d) => <option key={d.id} value={d.id}>{d.departmentName}</option>)}
          </select>
        )}
        <select className="input lg:w-52" value={type} onChange={(e) => setType(e.target.value)} aria-label="Reward type">
          <option value="">All types</option>
          {types.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
        </select>
        <input type="date" className="input lg:w-44" value={from} onChange={(e) => setFrom(e.target.value)} aria-label="From date" />
        <input type="date" className="input lg:w-44" value={to} onChange={(e) => setTo(e.target.value)} aria-label="To date" />
        {(from || to || type || departmentId) && (
          <button className="btn-secondary" onClick={() => { setFrom(''); setTo(''); setType(''); setDepartmentId('') }}>Clear</button>
        )}
      </div>
      {dateInvalid && <p className="text-xs text-amber-700 mb-3">"From" must be on or before "To".</p>}

      <div className="grid grid-cols-3 gap-4 mb-6">
        <StatCard label="Rewards" value={totals.count} icon={Award} accent="navy" />
        <StatCard label="Points awarded" value={totals.points} icon={Star} accent="amber" />
        <StatCard label="People recognised" value={totals.people} icon={Users} accent="emerald" />
      </div>

      <Tabs tabs={[{ key: 'recognitions', label: 'Recognitions' }, { key: 'leaderboard', label: 'Points leaderboard' }]} active={tab} onChange={setTab} />

      <div className="card overflow-hidden">
        {loading ? (
          <Spinner label="Loading rewards..." />
        ) : error ? (
          <ErrorState message="Could not load rewards." onRetry={load} />
        ) : tab === 'recognitions' ? (
          rewards.length === 0 ? (
            <EmptyState title="No rewards found" description="Grant a reward or adjust the filters." />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 border-b border-slate-200">
                  <tr>
                    {['Date', 'Employee', 'Reward', 'Points', 'Awarded by', ''].map((h) => (
                      <th key={h} className="text-left px-4 py-3 font-medium text-slate-500 whitespace-nowrap">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {rewards.map((r) => (
                    <tr key={r.id} className="hover:bg-slate-50 align-top">
                      <td className="px-4 py-3 text-slate-500 whitespace-nowrap">{formatDate(r.awardedDate)}</td>
                      <td className="px-4 py-3">
                        <p className="font-medium text-slate-800">{r.employee.fullName}</p>
                        <p className="text-xs text-slate-400">{r.department.departmentName}</p>
                      </td>
                      <td className="px-4 py-3">
                        <RewardTypeBadge type={r.type} label={r.typeLabel} />
                        <p className="text-slate-700 mt-1">{r.title}</p>
                        {r.citation && <p className="text-xs text-slate-400 mt-0.5 max-w-xs">{r.citation}</p>}
                      </td>
                      <td className="px-4 py-3 font-semibold text-amber-600">+{r.points}</td>
                      <td className="px-4 py-3 text-slate-500">{r.awardedBy?.fullName || r.awardedBy?.email}</td>
                      <td className="px-4 py-3 text-right">
                        {canRevoke(r) && (
                          <button className="p-1.5 rounded hover:bg-red-50 text-red-500" title="Revoke" aria-label="Revoke reward" onClick={() => setRevokeTarget(r)}>
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        ) : board.length === 0 ? (
          <EmptyState title="No points yet" description="Points appear here once rewards are granted." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  {['#', 'Employee', 'Department', 'Rewards', 'Total points'].map((h) => (
                    <th key={h} className="text-left px-4 py-3 font-medium text-slate-500">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {board.map((b) => (
                  <tr key={b.employee.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 text-slate-500">{b.rank}</td>
                    <td className="px-4 py-3 font-medium text-slate-800">{b.employee.fullName}</td>
                    <td className="px-4 py-3 text-slate-500">{b.department.departmentName}</td>
                    <td className="px-4 py-3 text-slate-500">{b.rewardCount}</td>
                    <td className="px-4 py-3 font-semibold text-amber-600">{b.totalPoints}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <GrantRewardModal open={grantOpen} onClose={() => setGrantOpen(false)} onGranted={load} />

      <ConfirmDialog
        open={!!revokeTarget}
        onClose={() => setRevokeTarget(null)}
        onConfirm={handleRevoke}
        loading={revoking}
        title="Revoke reward?"
        message={revokeTarget ? `This removes "${revokeTarget.title}" and its ${revokeTarget.points} points from ${revokeTarget.employee.fullName}.` : ''}
        confirmLabel="Revoke"
      />
    </div>
  )
}
