package repository;

import model.Employee;

public class EmployeeRepository extends CsvRepository<Employee> {

    private static final String FILE_PATH = "data/employees.csv";

    @Override
    protected String getFilePath() {
        return FILE_PATH;
    }

    @Override
    protected Employee createEntity() {
        return new Employee();
    }

    @Override
    protected String getHeader() {
        return "empId,fullName,deptId,empType,baseSalary,taxRate,joinDate,status";
    }

    @Override
    protected boolean matchesField(
            Employee employee,
            String fieldName,
            String value) {

        switch (fieldName.toLowerCase()) {

            case "id":
            case "empid":
                return value.equals(employee.getId());

            default:
                return false;
        }
    }
}