select u.id, u.user_name
from users u
where u.status = :status
order by u.user_name asc
