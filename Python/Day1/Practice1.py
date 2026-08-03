employees = [
{"name": "Alice", "department": "Engineering", "age": 30, "salary": 85000},
{"name": "Bob", "department": "Marketing", "age": 25, "salary": 60000},
{"name": "Charlie", "department": "Engineering", "age": 35, "salary": 95000},
{"name": "David", "department": "HR", "age": 45, "salary": 70000},
{"name": "Eve", "department": "Engineering", "age": 28, "salary": 78000},
]

eng_high_salary = [emp["name"] for emp in employees
    if emp["department"] == "Engineering" and emp["salary"] >= 80000]

print(eng_high_salary)

over_30 = [(emp["name"], emp["department"]) for emp in employees if emp["age"] >= 30]

print(over_30)

sorted_by_salary = sorted(employees, key=lambda x: x["salary"], reverse=True)
top3 = [(emp["name"], emp["salary"]) for emp in sorted_by_salary[:3]]

print(top3)