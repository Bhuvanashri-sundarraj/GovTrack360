import React, { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Award, Star, TrendingUp } from 'lucide-react'
import Spinner from '../components/Spinner'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import StatCard from '../components/StatCard'
import RewardTypeBadge from '../components/RewardTypeBadge'
import { getMyRewards, getRewardWall } from '../services/rewardService'
import { formatDate } from '../utils/performance'

export default function MyRewards() {
  const [mine, setMine] = useState(null)
  const [wall, setWall] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      const [m, w] = await Promise.all([getMyRewards(), getRewardWall(10)])
      setMine(m)
      setWall(w)
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  if (loading) return <Spinner label="Loading rewards..." />
  if (error) return <ErrorState message="Could not load rewards." onRetry={load} />

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">My Rewards</h1>
          <p className="text-sm text-slate-500 mt-1">Recognition you have earned, and who else is being celebrated.</p>
        </div>
        <Link to="/performance" className="btn-secondary self-start"><TrendingUp className="w-4 h-4" /> My performance</Link>
      </div>

      <div className="grid grid-cols-2 gap-4 mb-6 max-w-md">
        <StatCard label="Total points" value={mine.totalPoints} icon={Star} accent="amber" />
        <StatCard label="Rewards earned" value={mine.totalRewards} icon={Award} accent="navy" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <div className="px-5 py-4 border-b border-slate-200">
            <h2 className="text-sm font-semibold text-slate-800">My rewards</h2>
          </div>
          {mine.rewards.length === 0 ? (
            <EmptyState title="No rewards yet" description="When an officer recognises your work, it will show up here." />
          ) : (
            <div className="divide-y divide-slate-100">
              {mine.rewards.map((r) => (
                <div key={r.id} className="px-5 py-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-medium text-slate-800">{r.title}</p>
                      <div className="mt-1"><RewardTypeBadge type={r.type} label={r.typeLabel} /></div>
                    </div>
                    <span className="text-sm font-semibold text-amber-600 whitespace-nowrap">+{r.points} pts</span>
                  </div>
                  {r.citation && <p className="text-sm text-slate-500 mt-2">{r.citation}</p>}
                  <p className="text-xs text-slate-400 mt-2">
                    {formatDate(r.awardedDate)} · from {r.awardedBy?.fullName || r.awardedBy?.email}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card">
          <div className="px-5 py-4 border-b border-slate-200">
            <h2 className="text-sm font-semibold text-slate-800">Recognition wall</h2>
          </div>
          {wall.length === 0 ? (
            <EmptyState title="Nothing here yet" />
          ) : (
            <div className="divide-y divide-slate-100">
              {wall.map((r) => (
                <div key={r.id} className="px-5 py-3">
                  <p className="text-sm text-slate-800">
                    <span className="font-medium">{r.employee.fullName}</span>
                    <span className="text-slate-400"> · {r.department.departmentName}</span>
                  </p>
                  <div className="flex items-center gap-2 mt-1">
                    <RewardTypeBadge type={r.type} label={r.typeLabel} />
                    <span className="text-xs text-slate-500">{r.title}</span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1">{formatDate(r.awardedDate)}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
