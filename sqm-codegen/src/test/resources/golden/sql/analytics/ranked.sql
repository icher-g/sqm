select row_number() over (partition by dept order by salary desc) as rn from employees
