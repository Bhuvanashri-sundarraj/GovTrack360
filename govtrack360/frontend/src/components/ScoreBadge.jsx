import React from 'react'
import { BAND_LABELS, BAND_STYLES } from '../utils/performance'

export default function ScoreBadge({ band }) {
  const key = band || 'NOT_RATED'
  return <span className={`badge ${BAND_STYLES[key] || BAND_STYLES.NOT_RATED}`}>{BAND_LABELS[key] || key}</span>
}
