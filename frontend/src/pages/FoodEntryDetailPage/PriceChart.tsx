import { useState } from 'react'
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Dot,
} from 'recharts'
import { centsToSgd } from '@/shared/utils/format'
import type { FoodHistoricalData } from '@/shared/types/api'

type TimeRange = '1M' | '6M' | '12M'

function generateMockPrices(
  dates: string[],
  consensusPrice: number,
): { date: string; price: number }[] {
  const seed = consensusPrice * 7
  return dates.map((date, i) => {
    const variation = Math.sin((i * 0.3 + seed) % 6.28) * 0.15
    const noise = ((i * 13 + seed * 3) % 100) / 1000
    const price = consensusPrice * (1 + variation + noise)
    return { date, price: Math.round(price) }
  })
}

export default function PriceChart({ history }: { history: FoodHistoricalData }) {
  const [range, setRange] = useState<TimeRange>('1M')

  const chartData = generateMockPrices(
    history.availableDates,
    history.sgCentsConsensusPrice,
  )

  const consensusSgd = centsToSgd(history.sgCentsConsensusPrice)

  return (
    <div className="rounded-xl border border-secondary-200 bg-white p-5">
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-xs font-medium uppercase tracking-wider text-secondary-400">
            Current Benchmark
          </p>
          <p className="text-3xl font-bold text-primary-700">{consensusSgd}</p>
        </div>
        <div className="inline-flex rounded-lg border border-secondary-200 bg-secondary-50 p-0.5">
          {(['1M', '6M', '12M'] as TimeRange[]).map((r) => (
            <button
              key={r}
              type="button"
              onClick={() => setRange(r)}
              className={`rounded-md px-3 py-1 text-xs font-medium transition-colors ${
                range === r
                  ? 'bg-primary-700 text-primary-50 shadow-sm'
                  : 'text-secondary-500 hover:text-primary-700'
              }`}
            >
              {r}
            </button>
          ))}
        </div>
      </div>

      <div className="h-64">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={chartData} margin={{ top: 5, right: 5, left: -20, bottom: 5 }}>
            <defs>
              <linearGradient id="priceGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="rgb(var(--primary-700))" stopOpacity={0.3} />
                <stop offset="95%" stopColor="rgb(var(--primary-700))" stopOpacity={0} />
              </linearGradient>
            </defs>
            <XAxis
              dataKey="date"
              tick={{ fontSize: 11, fill: 'rgb(var(--secondary-400))' }}
              tickLine={false}
              axisLine={false}
              tickFormatter={(d) => {
                const date = new Date(d)
                return date.toLocaleDateString('en-SG', { month: 'short', day: 'numeric' })
              }}
            />
            <YAxis
              tick={{ fontSize: 11, fill: 'rgb(var(--secondary-400))' }}
              tickLine={false}
              axisLine={false}
              tickFormatter={(v) => centsToSgd(v)}
              domain={['dataMin - 50', 'dataMax + 50']}
            />
            <Tooltip
              contentStyle={{
                borderRadius: '8px',
                border: '1px solid rgb(var(--secondary-200))',
                boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
              }}
              formatter={(value) => [centsToSgd(value as number), 'Price']}
              labelFormatter={(label) => {
                const date = new Date(label)
                return date.toLocaleDateString('en-SG', {
                  weekday: 'short',
                  month: 'short',
                  day: 'numeric',
                  year: 'numeric',
                })
              }}
            />
            <Area
              type="monotone"
              dataKey="price"
              stroke="rgb(var(--primary-700))"
              strokeWidth={2}
              fill="url(#priceGradient)"
              dot={(props) => {
                const { cx, cy } = props
                return <Dot cx={cx} cy={cy} r={3} fill="rgb(var(--primary-700))" stroke="white" strokeWidth={2} />
              }}
              activeDot={{ r: 5, fill: 'rgb(var(--primary-700))', stroke: 'white', strokeWidth: 2 }}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
