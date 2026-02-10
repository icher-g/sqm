select
    u.id,
    u.org_id,
    count(*) as total_orders,
    sum(o.amount) as total_amount,
    row_number() over (partition by u.org_id order by o.created_at desc) as org_rank
from users u
left join orders o on o.user_id = u.id
where u.status = :status
  and o.created_at >= '2024-01-01'
  and o.created_at < '2025-01-01'
  and o.kind in ('A', 'B')
group by u.id, u.org_id
having count(*) > 1
order by sum(o.amount) desc nulls last, u.id asc
limit 100
offset 10
for update of u, o skip locked
